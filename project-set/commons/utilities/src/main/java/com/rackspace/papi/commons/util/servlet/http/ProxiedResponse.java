package com.rackspace.papi.commons.util.servlet.http;

import java.io.IOException;
import java.io.InputStream;

public interface ProxiedResponse {
    InputStream getInputStream() throws IOException;
    void close() throws IOException;
}
