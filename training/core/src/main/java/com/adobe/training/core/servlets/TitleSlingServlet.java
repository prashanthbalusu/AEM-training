package com.adobe.training.core.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

//@SlingServlet(paths="/bin/trainingproject/titleservlet")
//TitleSlindServlet using resourceTypes and extensions
@SlingServlet(resourceTypes="/apps/trainingproject/components/content/title", extensions="html")


public class TitleSlingServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Content-Type", "text/html");
        response.getWriter().print("<h1>Sling Servlet injected this title</h1>");
        response.getWriter().close();
    }
}
