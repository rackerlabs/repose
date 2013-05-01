/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.components.compression;


import com.rackspace.external.pjlcompression.CompressingFilter;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import javax.servlet.http.HttpServletRequest;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import org.slf4j.LoggerFactory;

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
      
      if(chain == null){
         myDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
         return myDirector;
      }
  
      try {
         
         filter.doFilter(mutableHttpRequest, response, chain);
         myDirector.setResponseStatusCode(response.getStatus());
      } catch (IOException ex) {
         LOG.error("IOException with Compression filter", ex);
         myDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
      } catch (ServletException ex) {
         LOG.error("Servlet error within Compression Filter", ex);
         myDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
      }
      return myDirector;

   }
}
