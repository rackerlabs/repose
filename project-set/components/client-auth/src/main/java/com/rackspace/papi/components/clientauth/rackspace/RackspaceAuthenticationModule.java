package com.rackspace.papi.components.clientauth.rackspace;

import com.rackspace.auth.v1_1.Account;
import com.rackspace.auth.v1_1.AuthenticationServiceClient;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import org.slf4j.Logger;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author jhopper
 */
public class RackspaceAuthenticationModule implements AuthModule {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RackspaceAuthenticationModule.class);
    private final AuthenticationServiceClient authenticationService;
    private final RackspaceAuth cfg;
    private final AccountUsernameExtractor accountUsernameExtractor;

    public RackspaceAuthenticationModule(RackspaceAuth cfg) {
        this.authenticationService = new AuthenticationServiceClient(cfg.getAuthenticationServer().getUri(), cfg.getAuthenticationServer().getUsername(), cfg.getAuthenticationServer().getPassword());
        this.cfg = cfg;
        this.accountUsernameExtractor = new AccountUsernameExtractor(cfg.getAccountMapping());
    }

    @Override
    public String getWWWAuthenticateHeaderContents() {
        return "RackAuth Realm=\"API Realm\"";
    }

    @Override
    public FilterDirector authenticate(HttpServletRequest request) {
        final FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
        filterDirector.setFilterAction(FilterAction.USE_MESSAGE_SERVICE);

        final String authToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.headerKey());
        final Account acct = accountUsernameExtractor.extract(request.getRequestURL().toString());

        boolean validToken = false;

        if ((!StringUtilities.isBlank(authToken) && acct != null) ) {
            try {
                validToken = authenticationService.validateToken(acct, authToken); //validateToken(acct, authToken);
            } catch (Exception ex) {
                LOG.error("Failure in auth: " + ex.getMessage(), ex);
                filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            }
        }

        setFilterDirectorValues(validToken, cfg.isDelegatable(), filterDirector, acct == null ? "" : acct.getUsername());
                      
        return filterDirector;
    }

    private void setFilterDirectorValues(boolean validToken, boolean isDelegatable, FilterDirector filterDirector, String accountUsername) {
        filterDirector.requestHeaderManager().putHeader(CommonHttpHeader.EXTENDED_AUTHORIZATION.headerKey(), "proxy " + accountUsername);

        if (validToken || isDelegatable) {
            filterDirector.setFilterAction(FilterAction.PASS);
        }

        if (validToken) {
            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.GROUPS.headerKey(), getGroupsListIds(accountUsername));
            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.USER.headerKey(), accountUsername);            
        }

        if (isDelegatable) {
            IdentityStatus identityStatus = IdentityStatus.Confirmed;

            if (!validToken) {
                identityStatus = IdentityStatus.Indeterminate;
            }

            filterDirector.requestHeaderManager().putHeader(CommonHttpHeader.IDENTITY_STATUS.headerKey(), identityStatus.name());
        }
    }

    private String[] getGroupsListIds(String accountUsername) {
        GroupsList groups = authenticationService.getGroups(accountUsername);
        int groupCount = groups.getGroup().size();

        String[] groupsArray = new String[groupCount];

        for (int i = 0; i < groupCount; i++) {
            groupsArray[i] = groups.getGroup().get(i).getId();
        }

        return groupsArray;
    }
}