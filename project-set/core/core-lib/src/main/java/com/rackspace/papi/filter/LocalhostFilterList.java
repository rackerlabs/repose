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
public class LocalhostFilterList extends PowerProxy {
    private final PowerProxy instance;

    public LocalhostFilterList(PowerProxy powerProxy) {
        this.instance = powerProxy;
    }

    public List<com.rackspace.papi.model.Filter> getFilters() {
        final String myHostname = getLocalHostName();
        List<com.rackspace.papi.model.Filter> thisHostsFilters = new ArrayList<Filter>();

        for (Host powerProxyHost : instance.getHost()) {
            if (powerProxyHost.getHostname().equals(myHostname)) {
                thisHostsFilters.addAll(powerProxyHost.getFilters().getFilter());
                break;
            }
        }

        return thisHostsFilters;
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