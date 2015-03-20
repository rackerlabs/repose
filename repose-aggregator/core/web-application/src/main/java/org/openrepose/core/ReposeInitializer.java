/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.powerfilter.EmptyServlet;
import org.openrepose.powerfilter.PowerFilter;
import org.openrepose.core.spring.CoreSpringProvider;
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
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();

        //Get the values out of the system properties that we'll need
        String configRoot = System.getProperty(
                ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.CONFIG_ROOT));
        boolean insecure = Boolean.parseBoolean(
                System.getProperty(ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.INSECURE), "false"));

        String clusterId = System.getProperty(ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.NODE.CLUSTER_ID));
        String nodeId = System.getProperty(ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.NODE.NODE_ID));

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
    }
}
