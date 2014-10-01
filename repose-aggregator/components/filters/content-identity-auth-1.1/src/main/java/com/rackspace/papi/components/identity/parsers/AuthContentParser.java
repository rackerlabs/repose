package com.rackspace.papi.components.identity.parsers;

import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;

import java.io.InputStream;

public interface AuthContentParser {

   AuthCredentials parse(InputStream stream);

   AuthCredentials parse(String content);
}
