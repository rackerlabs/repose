
package com.rackspace.papi.http;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.header.UserAgentExtractor;
import com.rackspace.papi.commons.util.net.NetUtilities;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;


public class ProxyHeadersGenerator {
   
   private ContainerConfigurationService configurationService;
   
   public ProxyHeadersGenerator(ContainerConfigurationService configurationService){
      this.configurationService = configurationService;
   }
   
   private void setVia(MutableHttpServletRequest request){
      
      String viaValue = configurationService.getVia();
      StringBuilder builder = new StringBuilder();
      UserAgentExtractor extractor = new UserAgentExtractor(request);   
      
      String userAgent = request.getProtocol();
      ExtractorResult<String> userAgentInfo = extractor.extractAgentInfo(userAgent);
      builder.append(userAgentInfo.getKey()).append(" ");
      
      if (StringUtilities.isBlank(viaValue)) {
         
         String server = request.getServletContext().getServerInfo();
         
         builder.append("Repose (").append(server).append(")");
      }else{
         builder.append(viaValue);
      }
      
      request.addHeader(CommonHttpHeader.VIA.toString(), builder.toString());
   }
   
   private void setXForwardedFor(MutableHttpServletRequest request){
      
      if(request.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString()) == null){
         request.addHeader(CommonHttpHeader.X_FORWARDED_FOR.toString(), request.getRemoteAddr());
      }
      
      request.addHeader(CommonHttpHeader.X_FORWARDED_FOR.toString(), NetUtilities.getLocalAddress());
   }
   
   public void setProxyHeaders(MutableHttpServletRequest request){
      
      setVia(request);
      setXForwardedFor(request);
   }
   
}
