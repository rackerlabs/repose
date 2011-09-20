package com.rackspace.papi.filter;

import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.servlet.PowerApiContextException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author franshua
 */
public class LocalhostFilterList {
    private final PowerProxy instance;

    public LocalhostFilterList(PowerProxy powerProxy) {
        this.instance = powerProxy;
    }

    public List<com.rackspace.papi.model.Filter> getFilters() {
        List<com.rackspace.papi.model.Filter> thisHostsFilters = new ArrayList<Filter>();
        
        Host localHost = getLocalHost();
        if (localHost != null) {
            thisHostsFilters.addAll(localHost.getFilters().getFilter());
        }

        return thisHostsFilters;
    }
    
    public Host getLocalHost() {
        final String myHostname = getLocalHostName();

        for (Host powerProxyHost : instance.getHost()) {
            if (powerProxyHost.getHostname().equals(myHostname)) {
              return powerProxyHost;
            }
        }
        
        return null;
    }

    public static String getLocalHostName() {
        try {
            final InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException e) {
            throw new PowerApiContextException("Failed to get hostname. Something weird is going on.", e);
        }
    }    
}