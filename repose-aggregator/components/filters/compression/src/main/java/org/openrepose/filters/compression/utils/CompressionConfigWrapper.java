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
package org.openrepose.filters.compression.utils;

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
