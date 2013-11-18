package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.model.Filter;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/27/11
 * Time: 12:52 PM
 */
public interface FilterContextManager {
    FilterContext loadFilterContext(Filter filter, Collection<EarClassLoaderContext> loadedApplications)
            throws ClassNotFoundException;
}
