package org.openrepose.commons.utils.servlet.http;

public interface RequestDestinations {

    void addDestination(String id, String uri, float quality);

    void addDestination(RouteDestination dest);

    RouteDestination getDestination();
}
