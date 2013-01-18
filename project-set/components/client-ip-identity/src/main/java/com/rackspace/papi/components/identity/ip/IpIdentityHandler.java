package com.rackspace.papi.components.identity.ip;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.net.IpAddressRange;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.ip.config.IpIdentityConfig;
import com.rackspace.papi.components.identity.ip.config.WhiteList;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class IpIdentityHandler extends AbstractFilterLogicHandler {

   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(IpIdentityHandler.class);
   public static final String DEFAULT_QUALITY = "0.1";
   private final IpIdentityConfig config;
   private final String quality;
   private WhiteList whiteList = new WhiteList();
   private final List<IpAddressRange> whitelistIps;

   public IpIdentityHandler(IpIdentityConfig config, List<IpAddressRange> whitelistIps) {
      this.config = config;
      this.whiteList = config.getWhiteList() == null ? new WhiteList() : config.getWhiteList();
      this.quality = determineQuality();
      this.whitelistIps = whitelistIps;
   }
   
   
   private String determineClientIp(HttpServletRequest request) {
      String address = request.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString());
      if (StringUtilities.isBlank(address)) {
         address = request.getRemoteAddr();
      } else {
         address = address.split(",")[0].trim();
      }

      return address;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      String address = determineClientIp(request);

      if (StringUtilities.isNotBlank(address)) {
         filterDirector.setFilterAction(FilterAction.PASS);

         String q = quality;
         String group = IpIdentityGroup.DEST_GROUP;
         try {
            if (onWhiteList(address)) {
               group = IpIdentityGroup.DEFAULT_WHITELIST_GROUP;
               q = ";q=" + whiteList.getQuality();
            }
         } catch (UnknownHostException ex) {
            LOG.warn("Invalid client IP Address: " + address);
         }

         headerManager.appendHeader(PowerApiHeader.USER.toString(), address + q);
         headerManager.appendHeader(PowerApiHeader.GROUPS.toString(), group + q);
      }
      return filterDirector;
   }

   private boolean onWhiteList(String address) throws UnknownHostException {
      boolean onList = false;

      byte[] addressBytes = InetAddress.getByName(address).getAddress();

      for (IpAddressRange range : whitelistIps) {
         if (range.addressInRange(addressBytes)) {
            onList = true;
            break;
         }
      }

      return onList;
   }

   private String determineQuality() {
      String q = DEFAULT_QUALITY;

      if (config != null && config.getQuality() != null) {
         q = config.getQuality().toString();
      }

      return ";q=" + q;
   }
}
