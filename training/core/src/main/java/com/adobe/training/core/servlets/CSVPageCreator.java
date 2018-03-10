package com.adobe.training.core.servlets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
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
 * This Servlet ingests a .csv file with the following columns:
 *
 * New Page Path, Page Title, Page Tag, Auto Publish, Template
 *
 * And outputs AEM pages created.
 *
 * Make sure /apps/trainingproject/tools/pagecreator/csv.html is added
 * Test with
 * http://localhost:4502/etc/trainingproject/pagecreator.csv.html
 *
 * It is assumed there is no header to the csv file
 *
 * To allow for POST requests for this importer the OSGi config
 * "Adobe Granite CSRF Filter" com.adobe.granite.csrf.impl.CSRFFilter
 * Needs to be configured with:
 * filter.excluded.paths=["/bin/trainingproject/pagecreator"]
 *
 * @author Kevin Nennig (nennig@adobe.com)
 */

@SlingServlet(resourceTypes="trainingproject/tools/pagecreator", selectors="csv", methods="POST")
public class CSVPageCreator extends SlingAllMethodsServlet{

    private static final long serialVersionUID = 1L;
    Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private Replicator replicator;

    private Resource resource;

    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)throws ServletException,IOException{
        response.setHeader("Content-Type", "application/json");
        JSONObject resultObject = new JSONObject();
        resource = request.getResource();

        String param = request.getParameter("importer");
        byte[] input = param.getBytes();
        InputStream stream = new ByteArrayInputStream(input);
        try {
            resultObject = readCSV(stream);
        } catch (JSONException e) {
            logger.error("Failure to Read CSV: " + e);
        }

        if(resultObject != null){
            //Write the result to the page
            response.getWriter().print(resultObject.toString());
            response.getWriter().close();
        }
    }

    /**
     * Reads the CSV file. The CSV file MUST be in the form of:
     *
     * JCR path, Page Title, Page Template, AEM Tag, Publish boolean
     *
     * @param stream Stream from the CSV
     * @return JSON object that contains the results of the page creation process
     */
    private JSONObject readCSV(InputStream stream) throws IOException, JSONException {
        JSONObject out = new JSONObject();
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        if(br != null){
            String line;
            String[] newPage;
            JSONObject createdPageObject = null;
            //Read each line of the CSV
            while ((line = br.readLine()) != null){
                newPage = line.split(",");
                String aemTag = null;
                String publishFlag = null;
                String aemTemplatePath = null;

                //If the line has a template, tag, publish flag, set those variables
                if(newPage.length == 5){
                    aemTemplatePath = newPage[2];
                    aemTag = newPage[3];
                    publishFlag = newPage[4];
                }else if(newPage.length == 4){
                    aemTemplatePath = newPage[2];
                    aemTag = newPage[3];
                }else if(newPage.length == 3){
                    publishFlag = newPage[2];
                }

                //As long as there is a path and title, the page can be created
                if((newPage.length  > 1)
                        && !newPage[0].isEmpty()
                        && !newPage[1].isEmpty()){
                    String path = newPage[0];
                    String title = newPage[1];
                    try {
                        createdPageObject = createTrainingPage(path, title, aemTemplatePath, aemTag, publishFlag);
                    } catch (Exception e) {
                        logger.error(path +" not created successfully: " + e);
                    }

                    //add the status of the row into the json array
                    if(createdPageObject != null){
                        out.put(path, createdPageObject); //Print Title of Page
                        createdPageObject = null;
                    }else{
                        out.put(path, new JSONObject().put("Status","Could not create a page"));
                    }
                }
                else {
                    out.put(line, new JSONObject().put("Status","Could not properly parce"));
                }
            }
        }
        br.close();
        return out;
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
