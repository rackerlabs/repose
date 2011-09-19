/*
 * 
 */
package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.model.Host;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Dan Daley
 */
public class HostWrapper implements Comparable<HostWrapper> {
    private static final String HTTP_PREFIX = "http";

    private final Host host;

    public HostWrapper(Host host) {
      this.host = host;
    }

    @Override
    public int compareTo(HostWrapper otherHost) {
        int result = host.getHostname().compareTo(otherHost.host.getHostname());
        if (result == 0) {
          result = Integer.valueOf(host.getServicePort()).compareTo(Integer.valueOf(otherHost.host.getServicePort()));
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }

      final HostWrapper other = (HostWrapper) obj;
      return compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 43 * hash + (host != null && host.getHostname() != null? host.getHostname().hashCode() : 0);
      return hash;
    }

    public String getAbsoluteUrl() throws MalformedURLException {
      return new URL(HTTP_PREFIX, host.getHostname(), host.getServicePort(), "").toExternalForm();
      //return StringUtilities.join(HTTP_PREFIX, host.getHostname(), ":", host.getServicePort());
    }
  
}
