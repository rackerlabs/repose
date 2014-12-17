package org.openrepose.core.services.classloader;

import org.openrepose.commons.utils.Destroyable;
import org.openrepose.commons.utils.classloader.EarClassLoaderContext;

import java.util.Collection;

public interface ClassLoaderManagerService extends Destroyable {

    boolean hasFilter(String contextName);
    Collection<EarClassLoaderContext> getLoadedApplications();
}
