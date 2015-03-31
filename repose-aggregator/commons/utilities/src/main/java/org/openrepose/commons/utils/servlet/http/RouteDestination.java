/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.servlet.http;

public class RouteDestination implements Comparable {

    private static final int BASE_HASH = 3;
    private static final int PRIME = 79;
    private final String destinationId;
    private final String uri;
    private final double quality;
    private String contextRemoved;

    public RouteDestination(String destinationId, String uri, double quality) {
        if (destinationId == null) {
            throw new IllegalArgumentException("destinationId cannot be null");
        }

        this.destinationId = destinationId;
        this.uri = uri != null ? uri : "";
        this.quality = quality;
    }

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

    @Override
    public int hashCode() {

        int hash = BASE_HASH;
        hash = PRIME * hash + (this.destinationId != null ? this.destinationId.hashCode() : 0);
        hash = PRIME * hash + (this.uri != null ? this.uri.hashCode() : 0);
        hash = PRIME * hash + (int) Double.doubleToLongBits(this.quality);
        return hash;
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
