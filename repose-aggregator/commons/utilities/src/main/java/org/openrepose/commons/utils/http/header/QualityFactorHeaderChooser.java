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
package org.openrepose.commons.utils.http.header;

import org.openrepose.commons.utils.StringUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author zinic
 */
public class QualityFactorHeaderChooser<T extends HeaderValue> implements HeaderChooser<T> {

    private final T defaultValue;

    public QualityFactorHeaderChooser() {
        defaultValue = null;
    }

    public QualityFactorHeaderChooser(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public T choosePreferredHeaderValue(Iterable<T> headerValues) {
        final Iterator<T> headerValueIterator = headerValues != null ? headerValues.iterator() : Collections.EMPTY_LIST.iterator();

        T prefered = defaultValue;

        while (headerValueIterator.hasNext()) {
            final T next = headerValueIterator.next();

            if (next != null && StringUtilities.isNotBlank(next.getValue())) {
                prefered = prefered == null || prefered.getQualityFactor() < next.getQualityFactor() ? next : prefered;

            }
        }

        return prefered;
    }

    @Override
    public List<T> choosePreferredHeaderValues(Iterable<T> headerValues) {
        final Iterator<T> headerValueIterator = headerValues != null ? headerValues.iterator() : Collections.EMPTY_LIST.iterator();
        final List<T> preferredHeaders = new ArrayList<T>();

        double currentQuality = -1;

        while (headerValueIterator.hasNext()) {
            final T next = headerValueIterator.next();

            if (next.getQualityFactor() > currentQuality) {
                preferredHeaders.clear();
                preferredHeaders.add(next);
                currentQuality = next.getQualityFactor();
            } else if (next.getQualityFactor() == currentQuality) {
                preferredHeaders.add(next);
            }
        }

        if (preferredHeaders.isEmpty() && defaultValue != null) {
            preferredHeaders.add((T) defaultValue);
        }

        return preferredHeaders;
    }
}
