package org.openrepose.core.service.proxy.httpcomponent;

import java.io.IOException;
import org.apache.http.client.methods.HttpRequestBase;

public interface HttpComponentProcessableRequest {
    HttpRequestBase process(HttpComponentRequestProcessor processor) throws IOException;
}
