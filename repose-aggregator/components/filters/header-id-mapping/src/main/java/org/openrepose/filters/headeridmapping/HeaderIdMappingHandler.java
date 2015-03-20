/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.filters.headeridmapping;

import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.regex.ExtractorResult;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.filters.headeridmapping.config.HttpHeader;
import org.openrepose.filters.headeridmapping.extractor.HeaderValueExtractor;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.HeaderManager;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


public class HeaderIdMappingHandler extends AbstractFilterLogicHandler {

   private final List<HttpHeader> sourceHeaders;

   public HeaderIdMappingHandler(List<HttpHeader> sourceHeaders) {
      this.sourceHeaders = sourceHeaders;
   }
   
   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      
      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      filterDirector.setFilterAction(FilterAction.PASS);

      ExtractorResult<String> result = new HeaderValueExtractor(request).extractUserGroup(sourceHeaders);
      
      if(!result.getResult().isEmpty()){
          headerManager.appendHeader(PowerApiHeader.USER.toString(), result.getResult());
          if (!result.getKey().isEmpty()) {
            headerManager.appendHeader(PowerApiHeader.GROUPS.toString(), result.getKey());
          }
      }
      
      return filterDirector;
   }
}
