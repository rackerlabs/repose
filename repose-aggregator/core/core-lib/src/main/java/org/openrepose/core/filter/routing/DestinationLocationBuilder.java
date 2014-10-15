package org.openrepose.core.filter.routing;

import org.openrepose.core.systemmodel.Destination;
import org.openrepose.core.systemmodel.Node;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;

public interface DestinationLocationBuilder {

    DestinationLocation build(Destination destination, String uri, HttpServletRequest request) throws MalformedURLException, URISyntaxException;
    void init(Node localhost);
}
