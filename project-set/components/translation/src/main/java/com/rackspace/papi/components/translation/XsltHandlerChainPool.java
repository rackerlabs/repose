package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.components.translation.xslt.handlerchain.XsltHandlerChain;

public class XsltHandlerChainPool {

    private final String contentType;
    private final String accept;
    private final Pool<XsltHandlerChain> pool;
    private final String resultContentType;

    public XsltHandlerChainPool(String contentType, String accept, String resultContentType, Pool<XsltHandlerChain> pool) {
        this.contentType = contentType;
        this.accept = accept;
        this.resultContentType = resultContentType;
        this.pool = pool;
    }

    public boolean accepts(String contentType, String accept) {
        return this.contentType.equalsIgnoreCase(contentType) && this.accept.equalsIgnoreCase(accept);
    }

    public String getContentType() {
        return contentType;
    }

    public String getAccept() {
        return accept;
    }

    public Pool<XsltHandlerChain> getPool() {
        return pool;
    }

    public String getResultContentType() {
        return resultContentType;
    }
}
