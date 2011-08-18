package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.HttpRequestInfoImpl;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.limits.schema.HttpMethod;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class RateLimitingRequestInfo extends HttpRequestInfoImpl {

    private final LinkedList<String> allUserGroups;
    private final HttpMethod requestMethod;
    private final String userName;

    public RateLimitingRequestInfo(HttpServletRequest request) {
        super(request);

        allUserGroups = new LinkedList<String>();

        for (Enumeration<String> groupHeaders = request.getHeaders(PowerApiHeader.GROUPS.headerKey()); groupHeaders.hasMoreElements();) {
            allUserGroups.add(groupHeaders.nextElement());
        }

        userName = request.getHeader(PowerApiHeader.USER.headerKey());
        requestMethod = HttpMethod.fromValue(request.getMethod().toUpperCase());

    }

    public String getFirstUserGroup() {
        return allUserGroups.isEmpty() ? null : allUserGroups.getFirst();
    }

    public List<String> getUserGroups() {
        return allUserGroups;
    }

    public String getUserName() {
        return userName;
    }

    public HttpMethod getRequestMethod() {
        return requestMethod;
    }
}
