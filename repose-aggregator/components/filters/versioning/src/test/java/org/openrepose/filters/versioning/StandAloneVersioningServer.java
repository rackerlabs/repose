package org.openrepose.filters.versioning;

import org.openrepose.powerfilter.PowerFilter;
import org.openrepose.commons.utils.jetty.JettyException;
import org.openrepose.commons.utils.jetty.JettyServerBuilder;
import org.openrepose.commons.utils.jetty.JettyTestingContext;
import org.openrepose.core.services.context.impl.PowerApiContextManager;
import org.openrepose.core.servlet.InitParameter;
import org.openrepose.commons.utils.test.DummyServlet;

public class StandAloneVersioningServer extends JettyTestingContext {

    public static void main(String[] args) {
        try {
            new StandAloneVersioningServer(2101);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public StandAloneVersioningServer(int port) throws JettyException {
        final JettyServerBuilder server = new JettyServerBuilder(port);
        buildServerContext(server);
        server.start();

        System.out.println("Server started");
    }

    @Override
    public final void buildServerContext(JettyServerBuilder serverBuilder) throws JettyException {
        try {
            serverBuilder.addContextListener(PowerApiContextManager.class);
        } catch (IllegalAccessException ex) {
            throw new JettyException(ex);
        } catch (InstantiationException ex) {
            throw new JettyException(ex);
        }
        serverBuilder.addContextInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), "/home/zinic/installed/etc/powerapi");
        serverBuilder.addFilter(PowerFilter.class, "/*");
        serverBuilder.addServlet(DummyServlet.class, "/*");
        serverBuilder.addServlet(DummyServlet.class, "/_v1.0/*");
        serverBuilder.addServlet(DummyServlet.class, "/_v2.0/*");
    }  
}
