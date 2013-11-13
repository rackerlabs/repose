package com.rackspace.papi.commons.util.servlet.http;

public interface RequestDestinations {

    void addDestination(String id, String uri, float quality);

    void addDestination(RouteDestination dest);

    RouteDestination getDestination();
}
