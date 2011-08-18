package com.rackspace.papi.commons.util.servlet.http;

import java.io.InputStream;
import javax.servlet.http.HttpServletResponse;

public interface ReadableHttpServletResponse extends HttpServletResponse {

    InputStream getBufferedOutputAsInputStream();
}
