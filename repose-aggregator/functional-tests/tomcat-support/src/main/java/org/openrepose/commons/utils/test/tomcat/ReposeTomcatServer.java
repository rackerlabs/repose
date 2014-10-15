package org.openrepose.commons.utils.test.tomcat;

import org.openrepose.commons.utils.test.ReposeContainerProps;
import org.openrepose.commons.utils.test.ReposeContainerUtil;
import org.apache.commons.cli.ParseException;

import javax.servlet.ServletException;

public class ReposeTomcatServer {

    public static void main(String[] args) throws ParseException, ServletException {

        ReposeContainerProps props = ReposeContainerUtil.parseArgs(args);
        ReposeTomcatContainer container = new ReposeTomcatContainer(props);
        container.startRepose();
    }
}
