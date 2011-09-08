package com.rackspace.papi.components.versioning.listener;

import com.rackspace.papi.commons.config.manager.LockedConfigurationUpdater;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/3/11
 * Time: 2:48 PM
 */
public abstract class SystemModelConfigurationListener extends LockedConfigurationUpdater<PowerProxy> {

    public SystemModelConfigurationListener(KeyedStackLock updateLock, Object updateKey) {
        super(updateLock, updateKey);
    }

    @Override
    public final void onConfigurationUpdated(PowerProxy configurationObject) {
        for (Host powerApiHost : configurationObject.getHost()) {
            onUpdate(powerApiHost);
        }
    }

    /**
     * When config is updated, this will be called to pass the update info to the caller (for closure).
     * @param powerApiHost data used for update
     */
    protected abstract void onUpdate(Host powerApiHost);
}
