/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.test.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.openrepose.commons.utils.test.ReposeContainer;
import org.openrepose.commons.utils.test.ReposeContainerProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;

public class ReposeTomcatContainer extends ReposeContainer {
    private static final Logger LOG = LoggerFactory.getLogger(ReposeTomcatContainer.class);
    private static final String BASE_DIRECTORY = System.getProperty("java.io.tmpdir");
    private Tomcat tomcat;


    public ReposeTomcatContainer(ReposeContainerProps props) throws ServletException {
        super(props);
        tomcat = new Tomcat();
        tomcat.setBaseDir(BASE_DIRECTORY);
        tomcat.setPort(Integer.parseInt(listenPort));
        tomcat.getHost().setAutoDeploy(true);
        tomcat.getHost().setDeployOnStartup(true);
        Context reposeContext = tomcat.addWebapp("/", warLocation);
        reposeContext.setCrossContext(true);
        reposeContext.addParameter("repose-cluster-id", props.getClusterId());
        reposeContext.addParameter("repose-node-id", props.getNodeId());

        String configDir = props.getConfigDirectory();
        if (configDir != null) {
            reposeContext.addParameter("repose-config-directory", configDir);
        }

        if (props.getOriginServiceWars() != null && props.getOriginServiceWars().length != 0) {
            for (String originService : props.getOriginServiceWars()) {
                tomcat.addWebapp("/" + getServletPath(originService), originService);
            }
        }
    }

    private static String getServletPath(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1, filePath.lastIndexOf('.'));
    }

    @Override
    @SuppressWarnings("squid:S106")
    protected void startRepose() {
        try {
            tomcat.start();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    stopRepose();
                }
            });

            // This method is intentionally trying to log to Standard Out as logging may not be up yet.
            // So it is safe to suppress warning squid:S106
            System.out.println("Tomcat Container Running");
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            LOG.trace("Unable To Start Tomcat Server", e);
        }
    }

    @Override
    @SuppressWarnings("squid:S106")
    protected void stopRepose() {
        try {
            // This method is intentionally trying to log to Standard Out as logging may not be up yet.
            // So it is safe to suppress warning squid:S106
            System.out.println("Stopping Tomcat Server");
            tomcat.stop();
            tomcat.getServer().stop();
        } catch (LifecycleException e) {
            LOG.trace("Error stopping Repose Tomcat", e);
        }
    }
}
