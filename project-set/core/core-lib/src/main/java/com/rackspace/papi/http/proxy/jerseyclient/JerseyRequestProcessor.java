package com.rackspace.papi.http.proxy.jerseyclient;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.regex.Pattern;

import static com.rackspace.papi.http.Headers.HOST;

/**
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 * 
 */
class JerseyRequestProcessor {

    private final HttpServletRequest sourceRequest;
    private final URI targetHost;

    public JerseyRequestProcessor(HttpServletRequest request, URI host) {
        this.sourceRequest = request;
        this.targetHost = host;
    }

    

    public WebResource setRequestParameters(WebResource method) {
        
        WebResource newMethod = method;

        if (!sourceRequest.getParameterMap().isEmpty()) {
            Pattern delimiter = Pattern.compile("&");
            Pattern pair = Pattern.compile("=");
            String[] params = delimiter.split(sourceRequest.getQueryString());

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
        final Enumeration<String> headerNames = sourceRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            final Enumeration<String> headerValues = sourceRequest.getHeaders(headerName);

            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                builder.header(headerName, processHeaderValue(headerName, headerValue));
            }
        }
    }

    private byte[] getData() throws IOException {
        final InputStream source = sourceRequest.getInputStream();

        if (source != null) {

            final BufferedInputStream httpIn = new BufferedInputStream(source);
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