/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.components.compression.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

public class CompressionConfigWrapper implements FilterConfig{
   
   FilterConfig config;
   Map<String, String> params;
   
   public CompressionConfigWrapper(FilterConfig config){
      this.config = config;
      wrap();
   }
   
   private void wrap(){
      
      params = new HashMap<String, String>();
      Enumeration<String> names = config.getInitParameterNames();
      
      while(names.hasMoreElements()){
         String name = names.nextElement();
         params.put(name, config.getInitParameter(name));
      }
   }

   @Override
   public String getFilterName() {
      return config.getFilterName();
   }

   @Override
   public ServletContext getServletContext() {
      return config.getServletContext();
   }

   @Override
   public String getInitParameter(String string) {
      return params.get(string);
   }

   @Override
   public Enumeration<String> getInitParameterNames() {
      return Collections.enumeration(params.keySet());
   }
   
   public void setInitParameter(String key, String value){
      params.put(key, value);
   }
   
   
}
