
package com.rackspace.papi.components.hnorm.util;

import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.*;
import java.util.List;
import java.util.regex.Pattern;


// TODO Find a better name for this
public class CompiledRegexAndList {
    
    private Pattern pattern;
    private List<HttpHeader> headerList;
    private List<HttpMethod> methodList;
    private Boolean isBlackList;
    
    public CompiledRegexAndList(String pattern, List<HttpHeader> headerList, List<HttpMethod> methodList, Boolean isBlackList){
        this.pattern = pattern==null ? Pattern.compile(".*") : Pattern.compile(pattern); //sets this as the catch-all
        this.headerList = headerList;
        this.methodList = methodList; //this will default to all if they do not provide it in the config
        this.isBlackList = isBlackList;
        
        if(methodList.isEmpty()){
            methodList.add(HttpMethod.ALL);
        }
    }

    public List<HttpMethod> getMethodList() {
        return methodList;
    }

    public Boolean isBlackList() {
        return isBlackList;
    }

    public List<HttpHeader> getHeaderList() {
        return headerList;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
