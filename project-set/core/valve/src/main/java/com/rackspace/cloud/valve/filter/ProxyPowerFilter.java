package com.rackspace.cloud.valve.filter;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.filter.PowerFilter;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.service.context.jndi.ContextAdapter;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import java.io.IOException;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Dan Daley
 */
public class ProxyPowerFilter extends PowerFilter {
  private String defaultRouteUrl = "http://localhost:8080";

  public ProxyPowerFilter() {
  }
    
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    super.init(new FilterConfigWrapper(filterConfig));
    
    defaultRouteUrl = getHostUrl(determineDefaultHost(getCurrentSystemModel().getHost()));
  }
  
  private Host determineDefaultHost(List<Host> hosts) {
    Host defaultHost = null;
    
    for (Host host: hosts) {
      if (host.getFilters() == null || host.getFilters().getFilter().isEmpty()) {
        defaultHost = host;
        break;
      }
    }
    
    return defaultHost;
  }
  
  private String getHostUrl(Host host) {
    // TODO we need to call this if the config is updated as well
    String url;
    if (host == null) {
      url = "http://localhost:8080";
    } else {
      StringBuilder hostString = new StringBuilder();
      hostString.append("http://")
              .append(host.getHostname())
              .append(":")
              .append(host.getServicePort());
      url = hostString.toString();
    }
    
    return url;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    // Set routeDestination to "default" route in case Versioning Filter is not running.
    // If versioning filter is running, it will replace this value.
    final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
    mutableHttpRequest.replaceHeader(PowerApiHeader.ROUTE_DESTINATION.headerKey(), defaultRouteUrl);
    
    super.doFilter(mutableHttpRequest, response, chain);
  }
  
  
}
