package org.openrepose.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.powerfilter.EmptyServlet;
import org.openrepose.powerfilter.PowerFilter;
import org.openrepose.core.spring.CoreSpringProvider;
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

        Config config = ConfigFactory.load("springConfiguration.conf");
        rootContext.scan(config.getString("powerFilterSpringContextPath"));

        servletContext.addListener(new ContextLoaderListener(rootContext));
        servletContext.addServlet("emptyServlet", EmptyServlet.class).addMapping("/*");
        servletContext.addFilter("springDelegatingFilterProxy", new DelegatingFilterProxy("powerFilter"))
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
    }
}
