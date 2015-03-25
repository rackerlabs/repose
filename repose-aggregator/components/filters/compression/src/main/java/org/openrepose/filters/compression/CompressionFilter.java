/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.compression;

import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class CompressionFilter implements Filter {

   private static final Logger LOG = LoggerFactory.getLogger(CompressionFilter.class);
   private static final String DEFAULT_CONFIG = "compression.cfg.xml";
   private String config;
   private CompressionHandlerFactory handlerFactory;
   private final ConfigurationService configurationService;

   @Inject
   public CompressionFilter(ConfigurationService configurationService) {
      this.configurationService = configurationService;
   }

   @Override
   public void doFilter(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
      CompressionHandler handler = handlerFactory.buildHandler();
      if(handler != null){
          handler.setFilterChain(fc);
      } else {
          LOG.error("Unable to build content compression handler");
      }
      new FilterLogicHandlerDelegate(sr, sr1, fc).doFilter(handler);
   }

   @Override
   public void destroy() {
      configurationService.unsubscribeFrom(config, handlerFactory);
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
      LOG.info("Initializing filter using config " + config);
      handlerFactory = new CompressionHandlerFactory(filterConfig);
      URL xsdURL = getClass().getResource("/META-INF/schema/config/content-compression-configuration.xsd");
      configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, ContentCompressionConfig.class);
   }
}
