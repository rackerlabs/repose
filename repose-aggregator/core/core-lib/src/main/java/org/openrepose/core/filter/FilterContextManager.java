package org.openrepose.core.filter;

import org.openrepose.commons.utils.classloader.ear.EarClassLoaderContext;
import org.openrepose.core.systemmodel.Filter;

import java.util.Collection;

public interface FilterContextManager {
    FilterContext loadFilterContext(Filter filter, Collection<EarClassLoaderContext> loadedApplications)
            throws FilterInitializationException;
}
