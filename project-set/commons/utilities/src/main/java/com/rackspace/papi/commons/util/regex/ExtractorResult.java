/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.util.regex;

import java.util.regex.Pattern;

/**
 *
 * @author malconis
 */
public class ExtractorResult {
    private final Pattern pattern;
    private final String result;

    public ExtractorResult(Pattern pattern, String result) {
        this.pattern = pattern;
        this.result = result;
    }
    
    

    public Pattern getPattern() {
        return pattern;
    }

    public String getResult() {
        return result;
    }
    
    
    
}
