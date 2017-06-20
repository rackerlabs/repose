package com.rackspace.lefty.tenant;

import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.filters.keystonev2.KeystoneRequestHandler;
import org.openrepose.filters.keystonev2.KeystoneV2Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConversions;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID;

/**
 * Created by adrian on 6/12/17.
 */
@Named
public class TenantCullingFilter implements Filter {

    public static final String RELEVANT_ROLES = "X-Relevant-Roles";

    private Logger log = LoggerFactory.getLogger(TenantCullingFilter.class);
    private Datastore datastore;

    @Inject
    public TenantCullingFilter(DatastoreService datastoreService) {
        datastore = datastoreService.getDefaultDatastore();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //do nothing
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequestWrapper request = new HttpServletRequestWrapper((HttpServletRequest) servletRequest);
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String cacheKey = request.getHeader(KeystoneV2Filter.AuthTokenKey());
        List<String> relevantRoles = request.getSplittableHeaders(RELEVANT_ROLES);
        if (cacheKey != null) {
            KeystoneRequestHandler.ValidToken token = (KeystoneRequestHandler.ValidToken) datastore.get(cacheKey);
            if (token != null) {
                List<String> tenants = new ArrayList<>();

                if (token.defaultTenantId().isDefined()) {
                    tenants.add(token.defaultTenantId().get());
                }

                JavaConversions.seqAsJavaList(token.roles())
                        .stream()
                        .filter(role -> relevantRoles.contains(role.name()))
                        .filter(role -> role.tenantId().isDefined())
                        .map(role -> role.tenantId().get())
                        .forEach(tenants::add);

                request.removeHeader(TENANT_ID);

                tenants.forEach(tenant -> request.addHeader(TENANT_ID, tenant));

                chain.doFilter(request, response);
            } else {
                log.debug("Cache miss for key: {}", cacheKey);
                response.sendError(SC_UNAUTHORIZED);
            }
        } else {
            log.debug("Cache key header not found");
            response.sendError(SC_UNAUTHORIZED);
        }
    }

    @Override
    public void destroy() {
        //do nothing
    }
}
