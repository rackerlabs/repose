package com.rackspace.papi.service.management;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 24, 2012
 * Time: 3:59:19 PM
 */
@Component("managementService")
public class ManagementServiceImpl implements ManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(ManagementServiceImpl.class);

    private Server server;

    @Override
    public void start(int managementPort, String artifactDirectory, String managementContext) {

        if (!isStarted()) {
            server = new Server(managementPort);

            WebAppContext webapp = new WebAppContext();
            webapp.setContextPath(managementContext);

            // Might need to dynamically build the war file name based on version
            // Do we ever want to version the manager separately?  Or will it always have
            // the same version as repose?
            webapp.setWar(artifactDirectory + "/repose_manager.war");
            server.setHandler(webapp);

            try {
                server.start();
                LOG.info("Repose REST Management API started: " + buildManagementUrl(managementPort, managementContext));
            } catch (Exception e) {
                LOG.error("Problem starting Repose REST Management API.");
            }
        }
    }

    private boolean isStarted() {
        return server != null;
    }

    private String buildManagementUrl(int managementPort, String managementContext) {
        StringBuilder builder = new StringBuilder("http://localhost:");

        builder.append(Integer.toString(managementPort));
        builder.append(managementContext);
        builder.append("/application.wadl");

        return builder.toString();
    }

    @Override
    public void stop() {
        if (isStarted()) {
            try {
                server.stop();
            } catch (Exception e) {
                LOG.error("Problem stopping Repose REST Management API.");
            }
        }
    }
}
