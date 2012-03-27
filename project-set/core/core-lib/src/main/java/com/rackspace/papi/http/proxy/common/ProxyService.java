/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rackspace.papi.http.proxy.common;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author zinic
 */
public interface ProxyService {
    int proxyRequest(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
