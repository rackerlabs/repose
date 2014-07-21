package com.rackspace.papi.test.tomcat;

import com.rackspace.papi.test.ReposeContainer;
import com.rackspace.papi.test.ReposeContainerProps;
import com.rackspace.papi.test.mocks.util.MocksUtil;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;

public class ReposeTomcatContainer extends ReposeContainer {
    private static final Logger LOG = LoggerFactory.getLogger(ReposeTomcatContainer.class);

    private Tomcat tomcat;
    private static final String BASE_DIRECTORY = System.getProperty("java.io.tmpdir");


    public ReposeTomcatContainer(ReposeContainerProps props) throws ServletException {
        super(props);
        tomcat = new Tomcat();
        tomcat.setBaseDir(BASE_DIRECTORY);
        tomcat.setPort(Integer.parseInt(listenPort));
        tomcat.getHost().setAutoDeploy(true);
        tomcat.getHost().setDeployOnStartup(true);
        tomcat.addWebapp("/", warLocation).setCrossContext(true);

        if(props.getOriginServiceWars() != null && props.getOriginServiceWars().length != 0){
            for(String originService: props.getOriginServiceWars()){
                tomcat.addWebapp("/"+ MocksUtil.getServletPath(originService), originService);
            }
        }
    }

    @Override
    protected void startRepose() {
        try {
            tomcat.start();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    stopRepose();
                }
            });

            System.out.println("Tomcat Container Running");
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            LOG.trace("Unable To Start Tomcat Server", e);
        }
    }

    @Override
    protected void stopRepose() {
        try {
            System.out.println("Stopping Tomcat Server");
            tomcat.stop();
            tomcat.getServer().stop();
        } catch (LifecycleException e) {
            LOG.trace("Error stopping Repose Tomcat", e);
        }
    }
}
