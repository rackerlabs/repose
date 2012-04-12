package com.rackspace.papi.components.identity.ip;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.ip.config.IpIdentityConfig;
import com.rackspace.papi.components.identity.ip.config.WhiteList;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;

public class IpIdentityHandler extends AbstractFilterLogicHandler {

   private final IpIdentityConfig config;
   private final String quality;
   public static final String DEFAULT_QUALITY = "0.1";

   private WhiteList whiteList = new WhiteList();

   public IpIdentityHandler(IpIdentityConfig config) {
      this.config = config;
      this.whiteList = config.getWhiteList() == null ? new WhiteList() : config.getWhiteList();
      this.quality = determineQuality();
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      String address = request.getRemoteAddr();

      if (!address.isEmpty()) {
         filterDirector.setFilterAction(FilterAction.PASS);

         String q = quality;
         String group = IpIdentityGroup.DEST_GROUP;         
         if (onWhiteList(address)) {
            group = IpIdentityGroup.DEFAULT_WHITELIST_GROUP;
            q = ";q=" + whiteList.getQuality();
         }
                  
         headerManager.appendHeader(PowerApiHeader.USER.toString(), address + q);
         headerManager.appendHeader(PowerApiHeader.GROUPS.toString(), group + q);
      }
      return filterDirector;
   }

   private boolean onWhiteList(String address) {
      boolean onList = false;

      for (String addressOnList : whiteList.getIpAddress()) {
         if (StringUtilities.nullSafeEqualsIgnoreCase(addressOnList, address)) {
            onList = true;
            break;
         }
      }
            
      return onList;
   }

   private String determineQuality() {
      String q = DEFAULT_QUALITY;

      if (config != null && config.getQuality() != null && !config.getQuality().trim().isEmpty()) {
         q = config.getQuality().trim();
      }

      return ";q=" + q;
   }
}
