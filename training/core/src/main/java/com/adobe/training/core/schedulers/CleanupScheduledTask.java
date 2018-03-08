package com.adobe.training.core.schedulers;


import java.util.Dictionary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true, label = "Training Cleanup Service")
@Service(value = Runnable.class)
@Property(name = "scheduler.expression", value = "*/20 * * * * ?") // Every 20
// seconds
public class CleanupScheduledTask implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private SlingRepository repository;
    @Property(label = "Path", description = "Delete this path", value = "/mypathtraining")
    public static final String CLEANUP_PATH = "cleanupPath";
    private String cleanupPath;

    protected void activate(ComponentContext componentContext) {
        configure(componentContext.getProperties());
    }

    protected void configure(Dictionary<?, ?> properties) {
        this.cleanupPath = PropertiesUtil.toString(properties.get(CLEANUP_PATH), null);
        logger.info("!!!configure: cleanupPath='{}''", this.cleanupPath);
    }

    @Override
    public void run() {
        logger.info("!!!running now");
        Session session = null;
        try {
            session = repository.loginService("training", null);
            if (session.itemExists(cleanupPath)) {
                session.removeItem(cleanupPath);
                logger.info("!!!node deleted");
                session.save();
            }
        } catch (RepositoryException e) {
            logger.error("!!!exception during cleanup", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }
}
