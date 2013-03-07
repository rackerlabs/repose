package com.rackspace.papi.jetty;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class JettyServerBuilderTest {

    public static class TestParent {

        int portnumber;
        JettyServerBuilder jettyServerBuilder;
        String name, value, pathspec;
        Class<? extends Filter> filterClass;
        Class<? extends Servlet> servletClass;


        @Before
        public void setUp() throws Exception {
            portnumber = 1;
            jettyServerBuilder = new JettyServerBuilder(portnumber);
            name = "test name";
            value = "test value";
            pathspec = "/pathspec";
            filterClass = null;
            servletClass = null;
        }

        @Test
        public void shouldContainServerObject() {
            assertNotNull(jettyServerBuilder.getServerInstance());
        }

        @Test
        public void shouldContainServletContextHandlerObject() {
            assertNotNull(jettyServerBuilder.getServletContextHandler());
        }

        @Test
        public void shouldAddContainContextInitParameter() {
            jettyServerBuilder.addContextInitParameter(name, value);
            assertTrue(jettyServerBuilder.getServletContextHandler().getInitParameter(name).equals(value));
        }

        @Test
        public void shouldAddFilter() {
            FilterHolder filterHolder = jettyServerBuilder.addFilter(filterClass, pathspec);
            assertNotNull(filterHolder);
        }

        @Test
        public void shouldAddServlet() {
            ServletHolder servletHolder = jettyServerBuilder.addServlet(servletClass, pathspec);
            assertNotNull(servletHolder);
        }

        @Test(expected = JettyException.class)
        public void shouldErrorOnStart() throws JettyException {
            jettyServerBuilder.start();
        }

        @Test(expected = Exception.class)
        public void shouldErrorOnStop() throws JettyException {
            jettyServerBuilder = null;
            jettyServerBuilder.stop();
        }
    }
}
