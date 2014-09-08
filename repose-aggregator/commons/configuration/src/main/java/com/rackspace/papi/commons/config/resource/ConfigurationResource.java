package com.rackspace.papi.commons.config.resource;

import java.io.IOException;
import java.io.InputStream;

public interface ConfigurationResource {

    boolean updated() throws IOException;

    boolean exists() throws IOException;

    String name();

    InputStream newInputStream() throws IOException;
}
