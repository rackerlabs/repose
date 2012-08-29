/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.rackspace.papi.commons.util.http.header;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author malconis
 */
public class UserAgentExtractor {

    private HttpServletRequest request;

    public UserAgentExtractor(HttpServletRequest request) {
        this.request = request;
    }

    public String extractAgent(String name) {

        if (StringUtilities.isBlank(name)) {
            return "";
        }

        String agent = request.getHeader(name);
        String[] agents = agent.split(",");

        //User Agent should only have 1 item listed and the item we need to extract from the Via header is the last listed
        return agent != null ? agents[agents.length - 1].trim() : "";
    }

    public ExtractorResult<String> extractAgentInfo(String header) {

        final String agent;
        final String version;

        String[] val = header.split("/");

        agent = val[0].trim();
        if (val.length > 1) {
            version = val[1].split("\\s")[0];
        } else {
            version = null;
        }

        return new ExtractorResult<String>(agent, version);

    }
}
