package org.openrepose.core.filter.routing;

import org.openrepose.core.systemmodel.Destination;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;

public interface LocationBuilder {

    DestinationLocation build(Destination destination, String uri, HttpServletRequest request) throws MalformedURLException, URISyntaxException;
}
