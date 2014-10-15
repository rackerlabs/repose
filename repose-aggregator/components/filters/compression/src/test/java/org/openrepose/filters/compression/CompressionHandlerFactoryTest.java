/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
   public void shouldInitializeCompressionHandlerFactory() {

      factory.configurationUpdated(config);
      assertTrue("Should initialize new content compression handler factory", factory.isInitialized());
   }

   @Test
   public void shouldReturnNullOnUnInitializedFactory() {

      assertNull("Should return null if no filter config or invalid config", factory.buildHandler());
   }

   @Test
   public void shouldBuildCompressionHandler() {

      factory.configurationUpdated(config);
      CompressionHandler handler = factory.buildHandler();
      assertNotNull("Should build new compression handler", handler);
   }
}
