package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpsURLConnectionSslInitializer;
import com.rackspace.papi.servlet.InitParameter;
import javax.inject.Named;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletContext;

/**
 * TODO: I'm worried this could be a race condition
 * These things happened in the PAPIContextManager, and need to happen early on, but I'm not sure before what
 * things. They're fired once, and then that's persistent throughout the entire JVM...
 */
@Named
public class ReposeStartupTasks {
    private ServletContext servletContext;

    @Inject
    public ReposeStartupTasks(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        //Determine if stuff has been set insecurely.
        final String insecureProp = InitParameter.INSECURE.getParameterName();
        final String insecure = System.getProperty(insecureProp, servletContext.getInitParameter(insecureProp));

        if (StringUtilities.nullSafeEqualsIgnoreCase(insecure, "true")) {
            new HttpsURLConnectionSslInitializer().allowAllServerCerts();
        }

        //Allows Repose to set any header to pass to the origin service. Namely the "Via" header
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }
}
