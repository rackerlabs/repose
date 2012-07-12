package com.rackspace.papi.commons.util.servlet.http;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

public interface ReadableHttpServletResponse extends HttpServletResponse {

    InputStream getBufferedOutputAsInputStream() throws IOException;
    String getMessage();
    InputStream getInputStream() throws IOException;
}
