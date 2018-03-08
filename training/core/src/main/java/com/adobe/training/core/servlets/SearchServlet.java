package com.adobe.training.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;

import com.day.cq.wcm.api.PageManager;

/**
 * Example URI: http://localhost:4502/content/trainingproject/en.search.sql.html?q=ipsum&wcmmode=disabled
 *
 * Example URI: http://localhost:4502/content/trainingproject/en.search.jqom.html?q=Lorem&wcmmode=disabled
 *
 * @author Kevin Nennig (nennig@adobe.com)
 */
@SlingServlet(resourceTypes = "trainingproject/components/structure/page", selectors="search")
public class SearchServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 3169795937693969416L;

    @Override
    public final void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Content-Type", "application/json");
        JSONObject jsonObject = new JSONObject();
        JSONArray resultArray = new JSONArray();

        try {
            //current node that is requested
            Node currentNode = request.getResource().adaptTo(Node.class);

            //cq:page node containing the requested node
            PageManager pageManager = request.getResource().getResourceResolver().adaptTo(PageManager.class);
            Node queryRoot = pageManager.getContainingPage(currentNode.getPath()).adaptTo(Node.class);

            String queryTerm = request.getParameter("q");
            if (queryTerm != null) {
                //get the selectors from the URI
                String[] selectors = request.getRequestPathInfo().getSelectors();
                ArrayList<String> language = new ArrayList<String>(Arrays.asList(selectors));

                //Search with JQOM or SQL depending on the selector
                NodeIterator searchResults = null;
                if(language.contains("jqom")) {
                    searchResults = performSearchWithJQOM(queryRoot, queryTerm);
                } else if(language.contains("sql")) {
                    searchResults = performSearchWithSQL(queryRoot, queryTerm);
                }

                //Add the search results into the json array
                if(searchResults != null) {
                    while (searchResults.hasNext()) resultArray.put(searchResults.nextNode().getPath());
                }
                jsonObject.put("results", resultArray);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        response.getWriter().print(jsonObject.toString());
        response.getWriter().close();

    }

    private NodeIterator performSearchWithJQOM(Node queryRoot, String queryTerm) throws RepositoryException {

        // JQOM infrastructure
        QueryObjectModelFactory qf = queryRoot.getSession().getWorkspace().getQueryManager().getQOMFactory();
        ValueFactory vf = queryRoot.getSession().getValueFactory();

        final String SELECTOR_NAME = "all results";
        final String SELECTOR_NT_UNSTRUCTURED = "nt:unstructured";
        // select all unstructured nodes
        Selector selector = qf.selector(SELECTOR_NT_UNSTRUCTURED, SELECTOR_NAME);

        // full text constraint
        Constraint constraint = qf.fullTextSearch(SELECTOR_NAME, null, qf.literal(vf.createValue(queryTerm)));
        // path constraint
        constraint = qf.and(constraint, qf.descendantNode(SELECTOR_NAME, queryRoot.getPath()));

        // execute the query without explicit order and columns
        QueryObjectModel query = qf.createQuery(selector, constraint, null, null);
        return query.execute().getNodes();

    }

    private NodeIterator performSearchWithSQL(Node queryRoot, String queryTerm) throws RepositoryException {
        QueryManager qm = queryRoot.getSession().getWorkspace().getQueryManager();
        Query query = qm.createQuery("SELECT * FROM [nt:unstructured] AS node WHERE ISDESCENDANTNODE(["
                + queryRoot.getPath() + "]) and CONTAINS(node.*, '" + queryTerm + "')", Query.JCR_SQL2);
        return query.execute().getNodes();
    }

}
