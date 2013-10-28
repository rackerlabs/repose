package com.rackspace.papi.test.tomcat;

import com.rackspace.papi.test.ReposeContainerProps;
import com.rackspace.papi.test.ReposeContainerUtil;
import org.apache.commons.cli.ParseException;

import javax.servlet.ServletException;

public class ReposeTomcatServer {

    public static void main(String[] args) throws ParseException, ServletException {

        ReposeContainerProps props = ReposeContainerUtil.parseArgs(args);


        ReposeTomcatContainer container = new ReposeTomcatContainer(props);


        container.startRepose();
    }
}
