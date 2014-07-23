package org.openrepose.components.flush;

import org.openrepose.core.service.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.HashMap;
import java.util.Map;

public class FlushOutputHandlerFactory extends AbstractConfiguredFilterHandlerFactory<FlushOutputHandler> {

    public FlushOutputHandlerFactory() {
    }

    @Override
    protected FlushOutputHandler buildHandler() {
        return new FlushOutputHandler();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>();
    }
}
