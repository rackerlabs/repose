package org.openrepose.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
 *  Programmatic initialization for the WAR deployment.
 *
 */
public class ReposeInitializer implements WebApplicationInitializer {
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.setParent(CoreSpringProvider.getInstance().getCoreContext());
        rootContext.setDisplayName("ReposeWARFileContext");

        Config config = ConfigFactory.load("springConfiguration.conf");
        rootContext.scan(config.getString("powerFilterSpringContextPath"));

        servletContext.addListener(new ContextLoaderListener(rootContext));
        servletContext.addServlet("emptyServlet", EmptyServlet.class).addMapping("/*");
        servletContext.addFilter("springDelegatingFilterProxy", new DelegatingFilterProxy("powerFilter"))
                      .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
    }
}
