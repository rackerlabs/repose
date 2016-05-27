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
package org.openrepose.commons.utils.logging.apache.format.stock;

import org.openrepose.commons.utils.logging.apache.format.converters.FormatConverter;
import org.openrepose.commons.utils.logging.apache.format.converters.TypeConversionFormatFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public abstract class HeaderHandler {
    private final String headerName;
    private final List<String> arguments;
    private String outputFormat;
    private String inputFormat;
    private FormatConverter converter;

    public HeaderHandler(String headerName, List<String> arguments) {
        this.headerName = headerName;
        this.arguments = arguments;
        checkArguments();
    }

    private void checkArguments() {
        if (arguments.size() > 0) {
            this.converter = TypeConversionFormatFactory.getConverter(arguments.get(0));
        }

        if (arguments.size() > 1) {
            this.outputFormat = arguments.get(1);
        }

        if (arguments.size() > 2) {
            this.inputFormat = arguments.get(2);
        }
    }

    protected String convert(String value) {
        if (converter != null) {
            // TODO handle quality?
            return converter.convert(value, inputFormat, outputFormat);
        }
        return value;
    }

    protected String getValues(Enumeration<String> values) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        while (values != null && values.hasMoreElements()) {
            if (!first) {
                builder.append(",");
            }
            builder.append(convert(values.nextElement()));
            first = false;
        }
        return builder.toString();
    }

    protected String getValues(Collection<String> values) {
        return getValues(Collections.enumeration(values));
    }

    public String getHeaderName() {
        return headerName;
    }

    public List<String> getArguments() {
        return arguments;
    }
}
