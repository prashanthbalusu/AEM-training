package com.adobe.training.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.training.core.models.StockModel;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

/**
 * To have this class trigger with every imported stock symbol,
 * make sure the workflow launcher has a Globbing Path of /content(/.* /)lastTrade (no spaces)
 *
 * This class assumes the following jcr data structure for the stock
 * /content
 *   + <stock symbol> [cq:Page]
 *     + lastTrade [nt:unstructured]
 *       - lastTrade = <imported stock value>
 */

@Service
@Component(metatype = false)
@Property(name = "process.label", value = "Stock Threshold Checker")
public class StockAlertProcess implements WorkflowProcess {

    private static final String TYPE_JCR_PATH = "JCR_PATH";
    private static final String TYPE_JCR_UUID = "JCR_UUID";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args)
            throws WorkflowException {
        try {
            // get the node the workflow is acting on
            Session session = workflowSession.getSession();

            //allows us to get the resourceResolver based on the current session
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("user.jcr.session", session);
            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = resourceResolverFactory.getResourceResolver(params);
            } catch (LoginException e) {
                logger.error("@@@@@LoginError" + e);
            }
            if(resourceResolver != null){
                WorkflowData data = workItem.getWorkflowData();
                Node payloadNode = null;
                String type = data.getPayloadType();
                //make sure the payload is a valid jcr path
                if(type.equals(TYPE_JCR_PATH) && data.getPayload() != null) {
                    String payloadData = (String) data.getPayload();
                    if(session.itemExists(payloadData)) {
                        payloadNode = session.getNode(payloadData);
                    }
                }
                else if (data.getPayload() != null && type.equals(TYPE_JCR_UUID)) {
                    payloadNode = session.getNodeByIdentifier((String) data.getPayload());
                }

                Resource stockResource =  resourceResolver.getResource(payloadNode.getParent().getPath());
                //Create StockModel from the resource
                StockModel stock = stockResource.adaptTo(StockModel.class);
                String symbol = stock.getStockSymbol();
                logger.info("@@@@@Checking stock: " + symbol);

                Double lastTrade;
                try {
                    lastTrade = stock.getLastTrade();
                    logger.info("@@@@@last trade was " + lastTrade);

                    //parse the passed workflow arguments into an iterator. Ex: "ADBE=105 \n MSFT=55"
                    Iterator<String> argumentsIterator = Arrays.asList(
                            Pattern.compile("\n").split(args.get("PROCESS_ARGS", ""))).iterator();

                    //Iterate over each argument
                    while (argumentsIterator.hasNext()) {
                        String argument = argumentsIterator.next();
                        //Check if the argument is the stock symbol in question
                        if(argument.contains(symbol)){
                            Double thresholdTrade = Double.parseDouble(argument.split("=")[1]);
                            //Check to see if the newly imported price is higher than the threshold
                            if (thresholdTrade < lastTrade) {
                                logger.warn("@@@@@ Stock Alert! " + symbol + " is over " + thresholdTrade);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
            else
            {
                logger.error("@@@@@ResourceResolver is null");
            }
        }
        catch (RepositoryException e) {
            logger.error("@@@@@RepositoryException", e);
        }
    }
}
