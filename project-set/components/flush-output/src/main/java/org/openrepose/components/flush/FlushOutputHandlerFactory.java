package org.openrepose.components.flush;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class FlushOutputHandlerFactory extends AbstractConfiguredFilterHandlerFactory<FlushOutputHandler> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FlushOutputHandler.class);

    public FlushOutputHandlerFactory() {
    }

    @Override
    protected FlushOutputHandler buildHandler() {
        return new FlushOutputHandler();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
        return updateListeners;
    }
}
