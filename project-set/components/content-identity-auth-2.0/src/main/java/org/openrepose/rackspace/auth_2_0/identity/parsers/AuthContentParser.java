package org.openrepose.rackspace.auth_2_0.identity.parsers;

import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;

import java.io.InputStream;

public interface AuthContentParser {
   public AuthCredentials parse(InputStream stream);
   public AuthCredentials parse(String content);
}
