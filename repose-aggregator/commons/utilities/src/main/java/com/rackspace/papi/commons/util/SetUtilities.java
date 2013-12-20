package com.rackspace.papi.commons.util;

import java.util.Set;

/**
 * @author fran
 */
public final class SetUtilities {
    private SetUtilities() {
        
    }
    
    public static <T> boolean nullSafeEquals(Set<T> one, Set<T> two) {
        return one == null ? (two == null) : (two != null) && one.equals(two);
    }
}
