package com.rackspace.papi.commons.util.servlet.http;

public class RouteDestination implements Comparable {

    private final String destinationId;
    private final String uri;
    private String contextRemoved;
    private final double quality;

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof RouteDestination)) {
            throw new IllegalArgumentException("Cannot compare to non RouteDestination instance");
        }

        RouteDestination r = (RouteDestination) o;

        int result = Double.compare(quality, r.quality);

        if (result == 0) {
            result = destinationId.compareTo(r.destinationId);
        }

        if (result == 0) {
            result = uri.compareTo(r.uri);
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RouteDestination)) {
            return false;
        }
        
        return compareTo(o) == 0;
    }
    private static final int BASE_HASH = 3;
    private static final int PRIME = 79;

    @Override
    public int hashCode() {

        int hash = BASE_HASH;
        hash = PRIME * hash + (this.destinationId != null ? this.destinationId.hashCode() : 0);
        hash = PRIME * hash + (this.uri != null ? this.uri.hashCode() : 0);
        hash = PRIME * hash + (int)Double.doubleToLongBits(this.quality);
        return hash;
    }

    public RouteDestination(String destinationId, String uri, double quality) {
        if (destinationId == null) {
            throw new IllegalArgumentException("destinationId cannot be null");
        }

        this.destinationId = destinationId;
        this.uri = uri != null ? uri : "";
        this.quality = quality;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public String getUri() {
        return uri;
    }

    public double getQuality() {
        return quality;
    }

    public String getContextRemoved() {
        return contextRemoved;
    }

    public void setContextRemoved(String contextRemoved) {
        this.contextRemoved = contextRemoved;
    }
}
