package com.rackspace.papi.commons.util.servlet.http.parser;

import com.rackspace.papi.commons.util.StringUtilities;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author fran
 */
// TODO: Eventually use JAXP to create a DOM for the httpx representation of HttpServletRequest
public class HttpRequestParser implements Parser<HttpServletRequest> {

    private static final String STATIC_BEGINNING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<httpx xmlns='http://docs.rackspace.com/httpx/v1.0' \n" +
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
            "    xsi:schemaLocation='http://docs.rackspace.com/httpx/v1.0 ./httpx.xsd'\n" +
            "    fidelity=\"HEAD BODY\"><request method=\"";

    private static final String STATIC_ENDING = "<body></body>\n" +
            "    </request>\n" +
            "</httpx>";

    @Override
    public String parse(HttpServletRequest request) {
        StringBuilder rootBuilder = new StringBuilder(STATIC_BEGINNING);

        rootBuilder.append(request.getMethod());
        rootBuilder.append("\" uri=\"");
        rootBuilder.append(request.getRequestURI());
        rootBuilder.append("\" version=\"");
        rootBuilder.append(request.getProtocol());
        rootBuilder.append("\">");

        String uriDetail = buildUriDetail(request.getParameterMap());
        String headers = buildHeaders(request);

        StringBuilder head = new StringBuilder();
        if (StringUtilities.isNotBlank(uriDetail) || StringUtilities.isNotBlank(headers)) {
            head.append("<head fidelity=\"URI_DETAIL HEADERS\">");
            head.append(uriDetail);
            head.append(headers);
        }

        head.append(STATIC_ENDING);

        rootBuilder.append(head);

        return rootBuilder.toString();
    }

    private String buildUriDetail(Map<String, String[]> parameterMap){
        String xmlUriDetail = "";

        if (parameterMap != null && !parameterMap.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder("<uri-detail>");

            for (Map.Entry<String, String[]> entry : parameterMap.entrySet())
            {
                stringBuilder.append("<query-parameter name=\"");
                stringBuilder.append(entry.getKey());
                stringBuilder.append("\">");

                for (String value : entry.getValue()) {
                    stringBuilder.append("<value>");
                    stringBuilder.append(value);
                    stringBuilder.append("</value>");
                }

                stringBuilder.append("</query-parameter>");
            }

            stringBuilder.append("</uri-detail>");

            xmlUriDetail = stringBuilder.toString();
        }
        
        return xmlUriDetail;
    }

    private String buildHeaders(HttpServletRequest request) {
        StringBuilder xmlHeaders = new StringBuilder();

        if (request.getHeaderNames().hasMoreElements()) {
            xmlHeaders.append("<headers fidelity=\"*\">");    
        }

        while (request.getHeaderNames().hasMoreElements()) {
            String headerName = request.getHeaderNames().nextElement();

            String headerValue = request.getHeader(headerName);

            xmlHeaders.append("<header name=\"");
            xmlHeaders.append(headerName);
            xmlHeaders.append("\">");
            xmlHeaders.append("<value>");
            xmlHeaders.append(headerValue);
            xmlHeaders.append("</value>");
            xmlHeaders.append("</header>");
        }

        if (xmlHeaders.length() > 1) {
            xmlHeaders.append("</headers>");
        }

        return xmlHeaders.toString();
    }
}
