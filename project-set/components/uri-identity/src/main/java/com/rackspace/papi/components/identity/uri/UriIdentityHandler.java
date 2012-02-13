package com.rackspace.papi.components.identity.uri;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.uri.config.UriIdentityConfig;
import com.rackspace.papi.components.identity.uri.config.IdentificationMapping;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;

public class UriIdentityHandler extends AbstractFilterLogicHandler {
   private final static String DEFAULT_GROUP = "User_Standard";
   private final UriIdentityConfig config;
   private final String quality;
   private final KeyedRegexExtractor<Object> keyedRegexExtractor;
   private final String group;

   public UriIdentityHandler(UriIdentityConfig config, String quality) {
      this.config = config;
      this.quality = quality;
      this.group = StringUtilities.getNonBlankValue(config.getGroup(), DEFAULT_GROUP);
      this.keyedRegexExtractor = new KeyedRegexExtractor<Object>();
      for (IdentificationMapping identificationMapping : config.getIdentificationMappings().getMapping()) {
         keyedRegexExtractor.addPattern(identificationMapping.getIdentificationRegex(), null);
      }
   }
   
   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

      final FilterDirector filterDirector = new FilterDirectorImpl();
      final HeaderManager headerManager = filterDirector.requestHeaderManager();
      final ExtractorResult<Object> userResult = keyedRegexExtractor.extract(request.getRequestURI());
      filterDirector.setFilterAction(FilterAction.PASS);

      if (userResult != null && !userResult.getResult().isEmpty()) {
         final String user = userResult.getResult();
         
         headerManager.appendHeader(PowerApiHeader.USER.toString(), user + quality);
         headerManager.appendHeader(PowerApiHeader.GROUPS.toString(), group);
      }

      return filterDirector;
   }
}
