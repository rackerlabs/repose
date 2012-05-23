package com.rackspace.papi.commons.util.servlet.http;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

public interface ReadableHttpServletResponse extends HttpServletResponse {

    InputStream getBufferedOutputAsInputStream();
}
