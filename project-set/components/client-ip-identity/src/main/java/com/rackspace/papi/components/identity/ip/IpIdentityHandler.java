package com.rackspace.papi.components.identity.ip;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.ip.config.IpIdentityConfig;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;

public class IpIdentityHandler extends AbstractFilterLogicHandler {

    private final IpIdentityConfig config;
    private final String quality;

    public IpIdentityHandler(IpIdentityConfig config, String quality) {
        this.config = config;
        this.quality = quality;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

        final FilterDirector filterDirector = new FilterDirectorImpl();
        HeaderManager headerManager = filterDirector.requestHeaderManager();
        String address = request.getRemoteAddr();

        if (!address.isEmpty()) {
            headerManager.appendHeader(PowerApiHeader.USER.toString(), address+quality);
            filterDirector.setFilterAction(FilterAction.PASS);
            String group = IpIdentityGroup.DEST_GROUP;
            headerManager.appendHeader(PowerApiHeader.GROUPS.toString(), group);
        }
        return filterDirector;
    }
}
