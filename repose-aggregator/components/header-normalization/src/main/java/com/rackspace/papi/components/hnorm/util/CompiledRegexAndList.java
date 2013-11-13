
package com.rackspace.papi.components.hnorm.util;

import com.rackspacecloud.api.docs.repose.header_normalization.v1.HttpHeader;
import com.rackspacecloud.api.docs.repose.header_normalization.v1.HttpMethod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


// TODO Find a better name for this
public class CompiledRegexAndList {
    
    private Pattern pattern;
    private List<HttpMethod> methodList;
    private Boolean isBlackList;
    private Set<String> filterList;
    
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

    public Set<String> getFilterList() {
        return filterList;
    }
    
    private void setFilterList(List<HttpHeader> headerList){
        
        filterList = new HashSet<String>();
        for(HttpHeader header : headerList){
            filterList.add(header.getId().toLowerCase());
        }
    }
}
