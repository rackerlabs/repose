package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.commons.util.StringUtilities;
import static com.rackspace.papi.http.Headers.HOST;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

/**
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 * 
 */
class JerseyRequestProcessor {

    private final URI targetHost;
    private final String queryString;
    private final Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private final InputStream inputStream;

    public JerseyRequestProcessor(HttpServletRequest request, URI host) throws IOException {
        this.queryString = request.getQueryString();
        this.targetHost = host;
        this.inputStream = request.getInputStream();

        final Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, Collections.list(request.getHeaders(headerName)));
        }
    }
    
    public JerseyRequestProcessor(URI host, String queryString, Map<String, List<String>> headers) {
        this(host, queryString, headers, null);
    }

    public JerseyRequestProcessor(URI host, String queryString, Map<String, List<String>> headers, InputStream inputStream) {
        this.targetHost = host;
        this.queryString = queryString;
        this.headers.putAll(headers);
        this.inputStream = inputStream;
    }

    private WebResource setRequestParameters(WebResource method) {
        
        WebResource newMethod = method;

        if (!StringUtilities.isEmpty(queryString)) {
            Pattern delimiter = Pattern.compile("&");
            Pattern pair = Pattern.compile("=");
            String[] params = delimiter.split(queryString);

            for (String param : params) {
                String[] paramPair = pair.split(param);
                if (paramPair.length == 2) {
                    newMethod = newMethod.queryParam(paramPair[0], paramPair[1]);
                }
            }
        }


        return newMethod;
    }

    /**
     * Scan header values and manipulate as necessary.  Host header, if provided,
     * may need to be updated.
     * 
     * @param headerName
     * @param headerValue
     * @return 
     */
    private String processHeaderValue(String headerName, String headerValue) {
        String result = headerValue;

        // In case the proxy host is running multiple virtual servers,
        // rewrite the Host header to ensure that we get content from
        // the correct virtual server
        if (headerName.equalsIgnoreCase(HOST.toString())) {
            result = targetHost.getHost() + ":" + targetHost.getPort();
        }

        return result;
    }

    /**
     * Copy header values from source request to the http method.
     * 
     * @param method 
     */
    private void setHeaders(Builder builder) {
        for (String header: headers.keySet()) {
            List<String> values = headers.get(header);
            for (String value: values) {
                builder.header(header, processHeaderValue(header, value));
            }
        }
    }

    private byte[] getData() throws IOException {
        if (inputStream != null) {

            final BufferedInputStream httpIn = new BufferedInputStream(inputStream);
            final ByteArrayOutputStream clientOut = new ByteArrayOutputStream();

            int readData;

            while ((readData = httpIn.read()) != -1) {
                clientOut.write(readData);
            }

            clientOut.flush();

            return clientOut.toByteArray();
        }

        return null;
    }

    /**
     * Process an entity enclosing http method.  These methods can handle
     * a request body.
     * 
     * @param method
     * @return
     * @throws IOException 
     */
    public Builder process(WebResource method) throws IOException {
        return process(setRequestParameters(method).getRequestBuilder());
    }

    public Builder process(Builder builder) throws IOException {

        setHeaders(builder);
        byte[] data = getData();
        if (data != null && data.length > 0) {
            builder.entity(data);
        }

        //method.entity(sourceRequest.getInputStream());
        return builder;
    }
}