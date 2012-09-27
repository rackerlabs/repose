package com.rackspace.auth.rackspace;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthToken;
import com.rackspace.papi.commons.util.regex.ExtractorResult;

import java.util.List;

/**
 * @author fran
 */
public interface AuthenticationService {
   AuthToken validateToken(ExtractorResult<String> account, String userToken);
   List<AuthGroup> getGroups(String userName);
}
