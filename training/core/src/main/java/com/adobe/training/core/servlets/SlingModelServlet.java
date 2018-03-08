package com.adobe.training.core.servlets;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.training.core.models.StockModel;

/**
 * Example URI http://localhost:4502/content/trainingproject/en.model.html/content/ADBE
 *
 * To use this servlet, a content structure must be created:
 * /content
 *   + ADBE [cq:Page]
 *     + lastTrade [nt:unstructured]
 *       - lastTrade = "100"
 *       - request	Date = "11/13/2016"
 *       - requestTime = "4:00pm"
 *
 * @author Kevin Nennig (nennig@adobe.com)
 */

@SlingServlet(resourceTypes = "trainingproject/components/structure/page", methods="GET", selectors="model")
public class SlingModelServlet extends SlingAllMethodsServlet{
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final long serialVersionUID = 1L;

    ResourceResolver resourceResolver;

    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)throws ServletException,IOException{
        response.setContentType("text/html");
        try {
            // Get the resource (a node in the JCR) using ResourceResolver from the request
            resourceResolver = request.getResourceResolver();

            //Specify the node of interest in the suffix on the request
            String nodePath = request.getRequestPathInfo().getSuffix();
            if(nodePath != null){
                Resource resource = resourceResolver.getResource(nodePath);

                // Adapt resource properties to variables using ValueMap, and log their values
                Resource parent = resource.getChild("lastTrade");
                ValueMap valueMap=parent.adaptTo(ValueMap.class);
                response.getOutputStream().println("<h3>");
                response.getOutputStream().println("lastTrade node with ValueMap is");
                response.getOutputStream().println("</h3><br />");
                response.getOutputStream().println("(Last Trade) "+valueMap.get("lastTrade").toString() + " (Requested Time) " + valueMap.get("requestDate").toString() + " " + valueMap.get("requestTime").toString());

                //Adapt the resource to our model
                StockModel stockModel = resource.adaptTo(StockModel.class);
                response.getOutputStream().println("<br /><h3>");
                response.getOutputStream().println("lastTrade node with StockModel is");
                response.getOutputStream().println("</h3><br />");
                response.getOutputStream().println("(Last Trade) " + stockModel.getLastTrade() + " (Requested Time) " + stockModel.getTimestamp());
            }
            else {
                response.getWriter().println("Can't get the last trade node, enter a suffix in the URI");
            }
        } catch (Exception e) {
            response.getWriter().println("Can't read last trade node. make sure the suffix path exists!");
            logger.error(e.getMessage());
        }
        response.getWriter().close();
    }

}
