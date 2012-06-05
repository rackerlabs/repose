package com.rackspace.papi.service.proxy.jersey;

import static com.rackspace.papi.http.Headers.HOST;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 *
 */
class JerseyRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(JerseyRequestProcessor.class);
    private final URI targetHost;
    private final HttpServletRequest request;
    private Pattern delimiter = Pattern.compile("&");
    private Pattern pair = Pattern.compile("=");

    public JerseyRequestProcessor(HttpServletRequest request, URI host) throws IOException {
        this.targetHost = host;
        this.request = request;
    }

    private WebResource setRequestParameters(WebResource method) {
        WebResource newMethod = method;
        final String queryString = request.getQueryString();

        if (queryString != null && queryString.length() > 0) {
            String[] params = delimiter.split(queryString);

            for (String param : params) {
                String[] paramPair = pair.split(param);
                if (paramPair.length == 2) {
                    String paramValue = paramPair[1];
                    try {
                        paramValue = URLDecoder.decode(paramValue, "UTF-8");
                    } catch (IllegalArgumentException ex) {
                        LOG.warn("Error decoding query parameter named: " + paramPair[0] + " value: " + paramValue, ex);
                    } catch (UnsupportedEncodingException ex) {
                        LOG.warn("Error decoding query parameter named: " + paramPair[0] + " value: " + paramValue, ex);
                    }
                    newMethod = newMethod.queryParam(paramPair[0], paramValue);
                }
            }
        }

        return newMethod;
    }

    /**
     * Scan header values and manipulate as necessary. Host header, if provided,
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
        final Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            
            Enumeration<String> values = request.getHeaders(header);
            while (values.hasMoreElements()) {
                String value = values.nextElement();
                builder.header(header, processHeaderValue(header, value));
            }
        }
    }

    private byte[] getData() throws IOException {
        InputStream inputStream = request.getInputStream();
        
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
     * Process an entity enclosing http method. These methods can handle a
     * request body.
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