
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
   
   private String getVia(MutableHttpServletRequest request){
      
      String viaValue = configurationService.getVia();
      StringBuilder builder = new StringBuilder();
      UserAgentExtractor extractor = new UserAgentExtractor(request);   
      
      String userAgent = request.getProtocol();
      ExtractorResult<String> userAgentInfo = extractor.extractAgentInfo(userAgent);
      builder.append(userAgentInfo.getKey()).append(" ");
      
      if (StringUtilities.isBlank(viaValue)) {
         
         String server = request.getServletContext().getServerInfo();
         
         ExtractorResult<String> serverInfo = extractor.extractAgentInfo(server);
         
         builder.append(serverInfo.getResult());
      }else{
         builder.append(viaValue);
      }
      
      return builder.toString();
   }
   
   public void setProxyHeaders(MutableHttpServletRequest request){
      
      request.addHeader(CommonHttpHeader.VIA.toString(), getVia(request));
      request.addHeader(CommonHttpHeader.X_FORWARDED_FOR.toString(), NetUtilities.getLocalAddress());
   }
   
}
