package com.rackspace.papi.filter;

import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.servlet.PowerApiContextException;
import com.rackspace.papi.commons.util.net.NetUtilities;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

/**
 * @author franshua
 */
public class SystemModelInterrogator {

    private final PowerProxy systemModel;

    public SystemModelInterrogator(PowerProxy powerProxy) {
        this.systemModel = powerProxy;
    }

    public Host getLocalHost() {
        final String myHostname = NetUtilities.getLocalHostName();
        Host localhost = null;

        for (Host powerProxyHost : systemModel.getHost()) {
            if (powerProxyHost.getHostname().equals(myHostname)) {
                localhost = powerProxyHost;
                break;
            }
        }

        return localhost;
    }

    // TODO: Enhancement - Explore using service domains to better handle routing identification logic
    public Host getNextRoutableHost() {
        final String myHostname = NetUtilities.getLocalHostName();
        final List<Host> hosts = systemModel.getHost();
        Host nextRoutableHost = null;
        
        for (Iterator<Host> hostIterator = hosts.iterator(); hostIterator.hasNext();) {
            final Host currentHost = hostIterator.next();
            
            if (currentHost.getHostname().equals(myHostname)) {
                nextRoutableHost = hostIterator.hasNext() ? hostIterator.next() : null;
                break;
            }
        }

        return nextRoutableHost;
    }

    // Moved getLocalHostName method to com.rackspace.papi.commons.util.net.NetUtilities;
    
}