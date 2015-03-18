/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.filters.compression;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Vector;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CompressionHandlerFactoryTest {

   private ContentCompressionConfig config;
   private Compression comp;
   private CompressionHandlerFactory factory;
   private FilterConfig filterConfig;

   public CompressionHandlerFactoryTest() {
   }

   @Before
   public void setUp() {

      comp = new Compression();
      comp.setCompressionThreshold(1024);
      comp.setDebug(Boolean.TRUE);
      comp.getIncludeContentTypes().add("application/xml");

      config = new ContentCompressionConfig();
      config.setCompression(comp);
      filterConfig = mock(FilterConfig.class);
      Vector empty = new Vector();
      when(filterConfig.getInitParameterNames()).thenReturn(empty.elements());
      when(filterConfig.getFilterName()).thenReturn("mockFilter");
      ServletContext ctx = mock(ServletContext.class);
      when(filterConfig.getServletContext()).thenReturn(ctx);
      factory = new CompressionHandlerFactory(filterConfig);
   }

   @Test
   public void shouldInitializeCompressionHandlerFactory() throws Exception {

      factory.configurationUpdated(config);
      assertTrue("Should initialize new content compression handler factory", factory.isInitialized());
   }

   @Test
   public void shouldReturnNullOnUnInitializedFactory() {

      assertNull("Should return null if no filter config or invalid config", factory.buildHandler());
   }

   @Test
   public void shouldBuildCompressionHandler() throws Exception {

      factory.configurationUpdated(config);
      CompressionHandler handler = factory.buildHandler();
      assertNotNull("Should build new compression handler", handler);
   }
}
