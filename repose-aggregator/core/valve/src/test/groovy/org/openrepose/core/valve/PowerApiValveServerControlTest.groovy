package org.openrepose.core.valve

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import spock.lang.Specification

public class PowerApiValveServerControlTest extends Specification {
    PowerApiValveServerControl powerApiValveServerControl
    CommandLineArguments commandLineArguments

    ListAppender app;

    def setup() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false)
        app = ((ListAppender)(ctx.getConfiguration().getAppender("List0"))).clear();
        commandLineArguments = new CommandLineArguments()
        commandLineArguments.setConfigDirectory("/")
        commandLineArguments.setHttpPort(9090)
        powerApiValveServerControl = new PowerApiValveServerControl(
                commandLineArguments.getHttpPort(),
                commandLineArguments.getHttpsPort(),
                commandLineArguments.getConfigDirectory(),
                commandLineArguments.getInsecure())
    }


    def "Should Start With Valid CLA"() {
        when:
        powerApiValveServerControl.startPowerApiValve()

        then:
        powerApiValveServerControl.serverInstance.isStarted()
    }

    def "Should Stop After Starting Successfully"() {
        when:
        powerApiValveServerControl.startPowerApiValve()
        powerApiValveServerControl.serverInstance.isStarted()
        powerApiValveServerControl.stopPowerApiValve()

        then:
        powerApiValveServerControl.serverInstance.isStopped()
        app.getEvents().find { it.getMessage().getFormattedMessage().contains("Repose has been stopped") }
    }
}
