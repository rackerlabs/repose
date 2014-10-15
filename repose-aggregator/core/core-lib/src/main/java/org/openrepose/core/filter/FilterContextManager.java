package org.openrepose.core.filter;

import org.openrepose.commons.utils.classloader.ear.EarClassLoaderContext;
import org.openrepose.core.systemmodel.Filter;
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
