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
package org.openrepose.commons.utils.http.normal;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zinic
 */
public class QueryParameter implements Comparable<QueryParameter> {

    private static final int HASH = 7 * 23;
    private final String name;
    private final List<String> values;

    public QueryParameter(String name) {
        this.name = name;
        this.values = new LinkedList<String>();
    }

    public String getName() {
        return name;
    }

    public void addValue(String value) {
        values.add(value);
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public int compareTo(QueryParameter o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof QueryParameter)) {
            return false;
        }

        return compareTo((QueryParameter) o) == 0;
    }

    @Override
    public int hashCode() {
        return HASH + (this.name != null ? this.name.hashCode() : 0);
    }
}
