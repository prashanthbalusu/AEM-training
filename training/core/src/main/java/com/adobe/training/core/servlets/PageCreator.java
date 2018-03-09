package com.adobe.training.core.servlets;

import java.io.IOException;

import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * This Servlet ingests a comma delimited string in the form of:
 *
 * New Page Path, Page Title, Page Tag, Auto Publish, Template
 *
 * And outputs the AEM page created.
 *
 * Make sure /apps/trainingproject/tools/pagecreator/pagecreator.html is added
 * Test with
 * http://localhost:4502/etc/trainingproject/pagecreator.html
 *
 *
 * To allow for POST requests for this importer the OSGi config
 * "Adobe Granite CSRF Filter" com.adobe.granite.csrf.impl.CSRFFilter
 * Needs to be configured with:
 * filter.excluded.paths=["/etc/trainingproject/pagecreator"]
 *
 * @author Kevin Nennig (nennig@adobe.com)
 */

@SlingServlet(resourceTypes="trainingproject/tools/pagecreator", methods="POST")
public class PageCreator extends SlingAllMethodsServlet{
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final long serialVersionUID = 1L;

    @Reference
    private Replicator replicator;

    private Resource resource;

    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)throws ServletException,IOException{
        response.setHeader("Content-Type", "application/json");
        JSONObject resultObject = new JSONObject();
        resource = request.getResource();

        String param = request.getParameter("importer");
        String[] newPage = param.split(",");
        try {
            resultObject = createTrainingPage(newPage[0], newPage[1], newPage[2], newPage[3], newPage[4]);
        } catch (Exception e) {
            logger.error("Failure to create page: " + e);
        }

        if(resultObject != null){
            //Write the result to the page
            response.getWriter().print(resultObject.toString());
            response.getWriter().close();
        }
    }

    /** Helper method to create the page based on available input
     *
     * @param path JCR location of the page to be created
     * @param title Page Title
     * @param template AEM Template this page should be created from. The template must exist in the JCR already.
     * @param tag Tag must already be created in AEM. The tag will be in the form of a path. Ex /etc/tags/marketing/interest
     * @param publish boolean to publish the page
     * @return
     */
    private JSONObject createTrainingPage(String path, String title, String template, String tag, String publish) throws Exception{
        JSONObject pageInfo = new JSONObject();

        if(path==null || title==null) return null;

        //Parse the path to get the pageNodeName and parentPath
        int lastSlash = path.lastIndexOf("/");
        String pageNodeName = path.substring(lastSlash+1);
        String parentPath = path.substring(0, lastSlash);

        if(pageNodeName==null || parentPath==null) return null;

        //Set a default template if none is given
        if(template == null || template.isEmpty()){
            template = "/apps/trainingproject/templates/page-content";
        }

        //Create page
        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        Page p = pageManager.create(parentPath,
                pageNodeName,
                template,
                title);
        //Add a tag to the page
        if(tag != null && !tag.isEmpty()) {
            //TagManager can be retrieved via adaptTo
            TagManager tm = resource.getResourceResolver().adaptTo(TagManager.class);
            tm.setTags(p.getContentResource(),
                    new Tag[]{tm.resolve(tag)},
                    true);
        }

        //Publish page if requested
        boolean publishPage = Boolean.parseBoolean(publish);
        if(publishPage){
            //Replicator is exposed as a service
            replicator.replicate(resource.getResourceResolver().adaptTo(Session.class),
                    ReplicationActionType.ACTIVATE,
                    p.getPath());
        }

        pageInfo.put("Status", "Successful");
        pageInfo.put("Location", p.getPath());
        pageInfo.put("Title", p.getTitle());
        pageInfo.put("Template Used", p.getTemplate().getPath());
        pageInfo.put("Tagged with", p.getTags()[0].getTitle());
        pageInfo.put("Was Published", publishPage);
        return pageInfo;
    }

}
