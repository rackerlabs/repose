package org.openrepose.commons.utils.logging.apache.format;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface FormatterLogic {
   String handle(HttpServletRequest request, HttpServletResponse response);
}
