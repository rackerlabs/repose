package org.openrepose.commons.utils.test.glassfish;

import org.openrepose.commons.utils.test.ReposeContainerProps;
import org.openrepose.commons.utils.test.ReposeContainerUtil;
import org.apache.commons.cli.ParseException;
import org.glassfish.embeddable.GlassFishException;

public class ReposeGlassfishServer {

    public static void main(String[] args) throws GlassFishException, ParseException {

        ReposeContainerProps props = ReposeContainerUtil.parseArgs(args);
        ReposeGlassFishContainer container = new ReposeGlassFishContainer(props);
        container.startRepose();
    }
}
