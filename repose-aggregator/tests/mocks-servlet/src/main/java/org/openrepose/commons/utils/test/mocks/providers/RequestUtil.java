/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.test.mocks.providers;

import org.openrepose.test.*;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * This takes the last parts we need out of MockUtils.
 *
 * Created by adrian on 2/22/16.
 */
public class RequestUtil {
    public static final String CONTEXT_PATH = "org.openrepose.test";

    public static String servletRequestToXml(HttpServletRequest request, String body) throws IOException, JAXBException {
        //formerly servletRequestToRequestInformation
        RequestInformation req = new RequestInformation();

        req.setUri(request.getRequestURL().toString());
        req.setPath(request.getRequestURI());
        req.setMethod(request.getMethod());
        req.setQueryString(request.getQueryString());
        req.setBody(body);

        if (!request.getParameterMap().isEmpty()) {
            QueryParameters q = new QueryParameters();
            Enumeration<String> queryParamNames = request.getParameterNames();
            while (queryParamNames.hasMoreElements()) {
                String name = queryParamNames.nextElement();
                String value = Arrays.toString(request.getParameterMap().get(name));
                NameValuePair nvp = new NameValuePair();
                nvp.setName(name);
                nvp.setValue(value);
                q.getParameter().add(nvp);
            }
            req.setQueryParams(q);
        }

        HeaderList h = new HeaderList();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                NameValuePair nvp = new NameValuePair();
                nvp.setName(headerName);
                nvp.setValue(headerValue);
                h.getHeader().add(nvp);
            }
        }
        req.setHeaders(h);

        //formerly requestInformationToXml
        ObjectFactory factory = new ObjectFactory();
        JAXBContext jaxbContext = JAXBContext.newInstance(CONTEXT_PATH);
        Marshaller marshaller = jaxbContext.createMarshaller();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaller.marshal(factory.createRequestInfo(req), baos);

        return baos.toString();
    }
}
