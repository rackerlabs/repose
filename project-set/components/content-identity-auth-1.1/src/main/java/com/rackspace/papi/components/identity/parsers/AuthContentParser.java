package com.rackspace.papi.components.identity.parsers;

import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import java.io.InputStream;

public interface AuthContentParser {
   public AuthCredentials parse(InputStream stream);
   public AuthCredentials parse(String content);
}
