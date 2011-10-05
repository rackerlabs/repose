package com.rackspace.papi.components.versioning;

import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.filter.PowerFilter;
import com.rackspace.papi.service.context.PowerApiContextManager;
import com.rackspace.papi.jetty.JettyServerBuilder;
import com.rackspace.papi.jetty.JettyTestingContext;
import com.rackspace.papi.test.DummyServlet;

public class StandAloneVersioningServer extends JettyTestingContext {

    public static void main(String[] args) {
        try {
            new StandAloneVersioningServer(2101);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public StandAloneVersioningServer(int port) throws Exception {
        final JettyServerBuilder server = new JettyServerBuilder(port);
        buildServerContext(server);
        server.start();

        System.out.println("Server started");
    }

    @Override
    public final void buildServerContext(JettyServerBuilder serverBuilder) throws Exception {
        serverBuilder.addContextListener(PowerApiContextManager.class);
        serverBuilder.addContextInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), "/home/zinic/installed/etc/powerapi");
        serverBuilder.addFilter(PowerFilter.class, "/*");
        serverBuilder.addServlet(DummyServlet.class, "/*");
        serverBuilder.addServlet(DummyServlet.class, "/_v1.0/*");
        serverBuilder.addServlet(DummyServlet.class, "/_v2.0/*");
    }  
}
