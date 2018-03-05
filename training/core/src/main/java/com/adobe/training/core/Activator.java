package com.adobe.training.core;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add this line to the configuration instructions for the maven-bundle-plugin in the POM
 * <Bundle-Activator>com.adobe.training.core.Activator</Bundle-Activator>
 */
public class Activator implements BundleActivator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        logger.info("##################Bundle Started##################");
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        logger.info("##################Bundle Stopped##################");
    }

}

