package org.openrepose.commons.utils.xslt;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;


@Plugin(name = "LogErrorListener", category = "Core", elementType = "appender", printObject = true)
public final class LogErrorListenerAppender extends AbstractAppender {

    protected LogErrorListenerAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    @PluginFactory
    public static LogErrorListenerAppender createAppender(@PluginAttribute("name") String name,
                                                          @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
                                                          @PluginElement("Layout") Layout layout,
                                                          @PluginElement("Filters") Filter filter) {
        if (name == null) {
            LOGGER.error("No name provided for LogErrorListenerAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new LogErrorListenerAppender(name, filter, layout, ignoreExceptions);
    }

    @Override
    public void append(LogEvent logEvent) {
        //
        // IF we receive anything other than one of the limited number of expected
        // messages from org.openrepose.commons.utils.xslt.LogErrorListener,
        // THEN we throw an AssertionError.
        //
        String msg = logEvent.getMessage().getFormattedMessage();
        if (logEvent.getLoggerName().equals("org.openrepose.commons.utils.xslt.LogErrorListener")) {
            if (!msg.contains("This is simply a warning") &&
                    !msg.contains("Throwing Error!") &&
                    !msg.contains("Fatal error while processing XSLT:") &&
                    !msg.contains("Termination forced by an xsl:message instruction")) {
                throw new AssertionError("Unexpected message from LogErrorListener: " + msg);
            }
        }
    }
}
