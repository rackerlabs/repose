package org.openrepose.commons.utils.logging.apache.format.stock;

import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RemoteUserHandler implements FormatterLogic {

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        return request.getHeader(PowerApiHeader.USER.toString());
    }
}
