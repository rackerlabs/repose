
package com.rackspace.papi.commons.util;

import java.lang.management.ManagementFactory;


public final class SystemUtils {
    
    private SystemUtils(){
    }
    
    public static String getPid(){
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }
}
