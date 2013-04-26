package com.rackspace.components.compression;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.components.compression.util.CompressionConfigWrapper;
import com.rackspace.papi.filter.FilterConfigWrapper;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.Map;
import javax.servlet.FilterConfig;
import com.planetj.servlet.filter.compression.CompressingFilter;
import com.rackspace.components.compression.util.CompressionParameters;
import com.rackspace.papi.commons.util.net.IpAddressRange;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class ContentCompressionHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ContentCompressionHandler> {

   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ContentCompressionHandlerFactory.class);
   private CompressionConfigWrapper config;
   private Compression contentCompressionConfig;
   private CompressingFilter filter;
   private ContentCompressionHandler handler;

   public ContentCompressionHandlerFactory(FilterConfig config) {

      this.config = new CompressionConfigWrapper(config);
      this.config.setInitParameter(CompressionParameters.STATS_ENABLED.getParam(), "true");
      this.config.setInitParameter(CompressionParameters.JAVA_UTIL_LOGGER.getParam(), LOG.getName());
      this.config.setInitParameter(CompressionParameters.DEBUG.getParam(), "true");
      try {


         filter = new CompressingFilter();
         filter.init(config);
      } catch (ServletException ex) {
         Logger.getLogger(ContentCompressionHandlerFactory.class.getName()).log(Level.SEVERE, null, ex);
      }
   }

   private class ContentCompressionConfigurationListener implements UpdateListener<ContentCompressionConfig> {

      private boolean isInitialized = false;

      @Override
      public void configurationUpdated(ContentCompressionConfig configurationObject) {
         contentCompressionConfig = configurationObject.getCompression();

         config.setInitParameter(CompressionParameters.STATS_ENABLED.getParam(), String.valueOf(contentCompressionConfig.isStatsEnabled()));
         config.setInitParameter(CompressionParameters.JAVA_UTIL_LOGGER.getParam(), LOG.getName());
         config.setInitParameter(CompressionParameters.DEBUG.getParam(), String.valueOf(contentCompressionConfig.isDebug()));
         config.setInitParameter(CompressionParameters.COMPRESSION_THRESHHOLD.getParam(), String.valueOf(contentCompressionConfig.getCompressionThreshold()));
         if (!contentCompressionConfig.getIncludeContentTypes().isEmpty()) {
            config.setInitParameter(CompressionParameters.INCLUDE_CONTENT_TYPES.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getIncludeContentTypes()));

         }
         if (!contentCompressionConfig.getIncludeContentTypes().isEmpty()) {
            config.setInitParameter(CompressionParameters.EXCLUDE_CONTENT_TYPES.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getExcludeContentTypes()));

         }
         
         if (!contentCompressionConfig.getIncludeUserAgentPatterns().isEmpty()) {
            config.setInitParameter(CompressionParameters.INCLUDE_USER_AGENT_PATTERNS.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getIncludeUserAgentPatterns()));

         }
         if (!contentCompressionConfig.getIncludeContentTypes().isEmpty()) {
            config.setInitParameter(CompressionParameters.INCLUDE_USER_AGENT_PATTERNS.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getExcludeUserAgentPatterns()));

         }
         
         
         filter = new CompressingFilter();
         try {
            filter.init(config);
            isInitialized = true;
         } catch (ServletException ex) {
            LOG.error("Unable to initialize content compression filter");
         }


      }

      @Override
      public boolean isInitialized() {
         return isInitialized;
      }
   }

   @Override
   protected ContentCompressionHandler buildHandler() {

      if (!this.isInitialized()) {
         return null;
      } else {
         return new ContentCompressionHandler(filter);
      }
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {
         {
            put(ContentCompressionConfig.class, new ContentCompressionConfigurationListener());
         }
      };
   }
}
