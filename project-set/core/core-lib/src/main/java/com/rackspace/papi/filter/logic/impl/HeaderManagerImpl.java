package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.filter.logic.HeaderApplicationLogic;
import com.rackspace.papi.filter.logic.HeaderManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author jhopper
 */
public class HeaderManagerImpl implements HeaderManager {

   private final Map<String, Set<String>> headersToAdd;
   private final Set<String> headersToRemove;

   public HeaderManagerImpl() {
      headersToAdd = new HashMap<String, Set<String>>();
      headersToRemove = new HashSet<String>();
   }

   private void applyTo(HeaderApplicationLogic applier) {
      // Remove headers first to make sure put logic stays sane
      for (String header : headersToRemove()) {
         applier.removeHeader(header);
      }

      for (Map.Entry<String, Set<String>> header : headersToAdd().entrySet()) {
         applier.addHeader(header.getKey(), header.getValue());
      }
   }

   @Override
   public void applyTo(final MutableHttpServletRequest request) {
      final HeaderApplicationLogic applicationLogic = new RequestHeaderApplicationLogic(request);
      applyTo(applicationLogic);
   }

   @Override
   public void applyTo(final HttpServletResponse response) {
      final ResponseHeaderApplicationLogic applicationLogic = new ResponseHeaderApplicationLogic(response);
      applyTo(applicationLogic);
   }

   @Override
   public boolean hasHeaders() {
      return !headersToAdd.isEmpty() || !headersToRemove.isEmpty();
   }

   @Override
   public Map<String, Set<String>> headersToAdd() {
      return headersToAdd;
   }

   @Override
   public Set<String> headersToRemove() {
      return headersToRemove;
   }

   @Override
   public void putHeader(String key, String... values) {
      // We remove the key first to preserve put logic such that any header put
      // will remove the header before setting new values
      headersToRemove.add(key.toLowerCase());
      
      headersToAdd.put(key.toLowerCase(), new HashSet<String>(Arrays.asList(values)));
   }

   @Override
   public void removeHeader(String key) {
      headersToRemove.add(key.toLowerCase());
   }

   @Override
   public void appendHeader(String key, String... values) {
      Set<String> headerValues = headersToAdd.get(key.toLowerCase());
      
      if (headerValues == null) {
         headerValues = new HashSet<String>();
         headersToAdd.put(key.toLowerCase(), headerValues);
      }
      
      headerValues.addAll(Arrays.asList(values));
   }

   private String valueWithQuality(String value, Double quality) {
      String result = value;
      if (quality != null) {
         result += ";q=" + quality;
      }
      return result;
   }

   @Override
   public void appendHeader(String key, String value, Double quality) {
      Set<String> headerValues = headersToAdd.get(key.toLowerCase());
      
      if (headerValues == null) {
         headerValues = new HashSet<String>();
         headersToAdd.put(key.toLowerCase(), headerValues);
      }
      
      headerValues.add(valueWithQuality(value, quality));
   }

   @Override
   public void appendToHeader(HttpServletRequest request, String key, String value) {
      final String currentHeaderValue = request.getHeader(key);

      if (currentHeaderValue != null) {
         this.putHeader(key.toLowerCase(), currentHeaderValue + "," + value);
      } else {
         this.putHeader(key.toLowerCase(), value);
      }
   }
}
