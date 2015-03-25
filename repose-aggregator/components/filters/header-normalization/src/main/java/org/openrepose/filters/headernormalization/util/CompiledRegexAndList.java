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

package org.openrepose.filters.headernormalization.util;

import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.filters.headernormalization.config.HttpHeader;
import org.openrepose.filters.headernormalization.config.HttpMethod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


// TODO Find a better name for this
public class CompiledRegexAndList {
    
    private Pattern pattern;
    private List<HttpMethod> methodList;
    private Boolean isBlackList;
    private Set<HeaderName> filterList;
    
    public CompiledRegexAndList(String pattern, List<HttpHeader> headerList, List<HttpMethod> methodList, Boolean isBlackList){
        //sets this as the catch-all
        this.pattern = pattern==null ? Pattern.compile(".*") : Pattern.compile(pattern); 
        //this will default to all if they do not provide it in the config
        this.methodList = methodList; 
        this.isBlackList = isBlackList;
        
        if(methodList.isEmpty()){
            methodList.add(HttpMethod.ALL);
        }
        setFilterList(headerList);
    }

    public List<HttpMethod> getMethodList() {
        return methodList;
    }

    public Boolean isBlackList() {
        return isBlackList;
    }

    

    public Pattern getPattern() {
        return pattern;
    }

    public Set<HeaderName> getFilterList() {
        return filterList;
    }
    
    private void setFilterList(List<HttpHeader> headerList){
        
        filterList = new HashSet<HeaderName>();
        for(HttpHeader header : headerList){
            filterList.add(HeaderName.wrap(header.getId()));
        }
    }
}
