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

import org.openrepose.commons.utils.http.CommonRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestDestinationsImpl implements RequestDestinations {

    private final List<RouteDestination> destinations;

    public RequestDestinationsImpl(HttpServletRequest request) {
        this.destinations = determineDestinations(request);
    }

    private List<RouteDestination> determineDestinations(HttpServletRequest request) {
        List<RouteDestination> result = (List<RouteDestination>) request.getAttribute(CommonRequestAttributes.DESTINATIONS);
        if (result == null) {
            result = new ArrayList<RouteDestination>();
            request.setAttribute(CommonRequestAttributes.DESTINATIONS, result);
        }

        return result;
    }

    @Override
    public void addDestination(String id, String uri, float quality) {
        addDestination(new RouteDestination(id, uri, quality));
    }

    @Override
    public void addDestination(RouteDestination dest) {
        if (dest == null) {
            throw new IllegalArgumentException("Destination cannot be null");
        }
        destinations.add(dest);
    }

    @Override
    public RouteDestination getDestination() {
        if (destinations.isEmpty()) {
            return null;
        }

        Collections.sort(destinations);
        return destinations.get(destinations.size() - 1);
    }
}
