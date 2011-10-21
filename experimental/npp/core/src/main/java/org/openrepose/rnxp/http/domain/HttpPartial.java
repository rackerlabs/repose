package org.openrepose.rnxp.http.domain;

/**
 *
 * @author zinic
 */
public class HttpPartial {

    private final HttpMessageComponent componentType;
    private String partial, headerKey, headerValue;
    private HttpMethod method;

    public HttpPartial(HttpMessageComponent componentType) {
        this.componentType = componentType;
    }

    public HttpMessageComponent messageComponent() {
        return componentType;
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
        this.partial = partial;
    }
}
