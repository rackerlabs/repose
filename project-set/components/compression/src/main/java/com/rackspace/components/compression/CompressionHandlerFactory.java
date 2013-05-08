package com.rackspace.components.compression;

import com.rackspace.external.pjlcompression.CompressingFilter;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.components.compression.util.CompressionConfigWrapper;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.Map;
import javax.servlet.FilterConfig;
import com.rackspace.components.compression.util.CompressionParameters;
import java.util.HashMap;
import javax.servlet.ServletException;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class CompressionHandlerFactory extends AbstractConfiguredFilterHandlerFactory<CompressionHandler> {

   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CompressionHandlerFactory.class);
   private CompressionConfigWrapper config;
   private Compression contentCompressionConfig;
   private CompressingFilter filter;

   public CompressionHandlerFactory(FilterConfig config) {

      this.config = new CompressionConfigWrapper(config);
      this.config.setInitParameter(CompressionParameters.STATS_ENABLED.getParam(), "true");
      this.config.setInitParameter(CompressionParameters.JAVA_UTIL_LOGGER.getParam(), LOG.getName());
      this.config.setInitParameter(CompressionParameters.DEBUG.getParam(), "true");
      try {

         filter = new CompressingFilter();
         filter.init(config);
      } catch (ServletException ex) {
         LOG.error("Unable to initialize CompressingFilter: ", ex);
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

         }else if (!contentCompressionConfig.getExcludeContentTypes().isEmpty()) {
            config.setInitParameter(CompressionParameters.EXCLUDE_CONTENT_TYPES.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getExcludeContentTypes()));

         }
         
         if (!contentCompressionConfig.getIncludeUserAgentPatterns().isEmpty()) {
            config.setInitParameter(CompressionParameters.INCLUDE_USER_AGENT_PATTERNS.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getIncludeUserAgentPatterns()));

         } else if (!contentCompressionConfig.getExcludeUserAgentPatterns().isEmpty()) {
            config.setInitParameter(CompressionParameters.EXCLUDE_USER_AGENT_PATTERNS.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getExcludeUserAgentPatterns()));
         }
            
         filter = new CompressingFilter();
         try {
            filter.init(config);
            filter.setForRepose();
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
   protected CompressionHandler buildHandler() {

      if (!this.isInitialized()) {
         return null;
      } else {
         return new CompressionHandler(filter);
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
