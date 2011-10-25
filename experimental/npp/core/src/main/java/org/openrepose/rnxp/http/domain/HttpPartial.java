package org.openrepose.rnxp.http.domain;

import java.io.InputStream;

/**
 *
 * @author zinic
 */
public class HttpPartial {

    private final HttpMessageComponent componentType;
    private String partial, headerKey, headerValue;
    private InputStream inputStream, outputStream;
    private HttpMethod method;

    public HttpPartial(HttpMessageComponent componentType) {
        this.componentType = componentType;
    }

    public HttpMessageComponent messageComponent() {
        return componentType;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(InputStream outputStream) {
        this.outputStream = outputStream;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getHeaderKey() {
        return headerKey;
    }

    public void setHeaderKey(String headerKey) {
        this.headerKey = headerKey;
    }

    public String getHeaderValue() {
        return headerValue;
    }

    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getPartial() {
        return partial;
    }

    public void setPartial(String partial) {
        switch (componentType) {
            case HEADER:
                final String[] splitPartial = partial.split(":", 2);

                if (splitPartial.length == 2) {
                    headerKey = splitPartial[0].trim().toLowerCase();
                    headerValue = splitPartial[1].trim().toLowerCase();
                } else {
                    // TODO: Error
                }

                break;
        }

        this.partial = partial;
    }
}
