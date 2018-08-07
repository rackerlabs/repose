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
package org.openrepose.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.openrepose.core.spring.CoreSpringProvider;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.powerfilter.EmptyServlet;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.EnumSet;

/**
 * Programmatic initialization for the WAR deployment.
 */
public class ReposeInitializer implements WebApplicationInitializer {
    private static final String CONFIG_ROOT = ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.CONFIG_ROOT);
    private static final String INSECURE = ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.INSECURE);
    private static final String CLUSTER_ID = ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.NODE.CLUSTER_ID);
    private static final String NODE_ID = ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.NODE.NODE_ID);

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();

        //Get the values out of the system properties that we'll need
        String configRoot = System.getProperty(CONFIG_ROOT);
        String clusterId = System.getProperty(CLUSTER_ID);
        String nodeId = System.getProperty(NODE_ID);
        String insecureString = System.getProperty(INSECURE);

        if (configRoot == null) {
            configRoot = servletContext.getInitParameter(CONFIG_ROOT);
            if (configRoot == null) {
                configRoot = "/etc/repose";
            }
        }
        if (clusterId == null) {
            clusterId = servletContext.getInitParameter(CLUSTER_ID);
        }
        if (nodeId == null) {
            nodeId = servletContext.getInitParameter(NODE_ID);
        }
        if (insecureString == null) {
            insecureString = servletContext.getInitParameter(INSECURE);
        }

        boolean insecure = Boolean.parseBoolean(insecureString);

        CoreSpringProvider csp = CoreSpringProvider.getInstance();
        csp.initializeCoreContext(configRoot, insecure);

        //The parent context is not the core spring context, but an instance of a node context
        //A war file is only ever one local node.
        rootContext.setParent(csp.getNodeContext(clusterId, nodeId));
        rootContext.setDisplayName("ReposeWARFileContext");

        PropertySourcesPlaceholderConfigurer propConfig = new PropertySourcesPlaceholderConfigurer();
        propConfig.setEnvironment(rootContext.getEnvironment());
        rootContext.addBeanFactoryPostProcessor(propConfig);

        Config config = ConfigFactory.load("springConfiguration.conf");
        rootContext.scan(config.getString("powerFilterSpringContextPath"));

        servletContext.addListener(new ContextLoaderListener(rootContext));
        servletContext.addServlet("emptyServlet", EmptyServlet.class).addMapping("/*");
        servletContext.addFilter("springDelegatingFilterProxy", new DelegatingFilterProxy("powerFilter"))
                .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

        // todo: when switching to the new ReposeFilterChain, replce the previous line with the following line
        // todo: to wire in the routing servlet
        // servletContext.addServlet("reposeRoutingServlet", rootContext.getBean(ReposeRoutingServlet.class)).addMapping("/*");
    }
}
