package com.rackspace.papi.commons.util.logging.apache.format;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface FormatArgumentHandler {

   String format(HttpServletRequest request, HttpServletResponse response);
}
