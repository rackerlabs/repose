/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openrepose.filters.compression;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.external.pjlcompression.CompressingFilter;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.EOFException;
import java.io.IOException;

public class CompressionHandler extends AbstractFilterLogicHandler {

   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CompressionHandler.class);
   CompressingFilter filter;
   FilterChain chain;

   public CompressionHandler(CompressingFilter filter) {
      this.filter = filter;
   }

   public void setFilterChain(FilterChain chain) {
      this.chain = chain;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

      final FilterDirector myDirector = new FilterDirectorImpl();
      final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
      myDirector.setFilterAction(FilterAction.RETURN);

      if (chain == null) {
         myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
         return myDirector;
      }

      try {
         filter.doFilter(mutableHttpRequest, response, chain);
         myDirector.setResponseStatusCode(response.getStatus());
      } catch (IOException ioe) {
         if ("Not in GZIP format".equalsIgnoreCase(ioe.getMessage())) {
            LOG.warn("Unable to decompress message. Bad request body or content-encoding");
            LOG.debug("Gzip Error: ", ioe);
            myDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_REQUEST);
         } else if(ioe.getClass() == EOFException.class) {
            LOG.warn("Unable to decompress message. Bad request body or content-encoding");
            LOG.debug("EOF Error: ", ioe);
            myDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_REQUEST);
         } else {
            LOG.error("IOException with Compression filter " + ioe.getClass(), ioe);
            myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
         }
      } catch (ServletException se) {
         LOG.error("Servlet error within Compression filter ", se);
         myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }

      return myDirector;
   }
}
