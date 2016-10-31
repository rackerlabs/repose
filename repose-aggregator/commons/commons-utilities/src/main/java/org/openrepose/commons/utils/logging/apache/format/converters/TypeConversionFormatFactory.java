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
package org.openrepose.commons.utils.logging.apache.format.converters;

import org.openrepose.commons.utils.StringUtilities;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public enum TypeConversionFormatFactory {

    DATE(DateTimeFormatConverter.class);

    TypeConversionFormatFactory(Class<? extends FormatConverter> converter) {
        try {
            ConverterMap.addConverter(name(), converter.newInstance());
        } catch (InstantiationException | IllegalAccessException ex) {
            LoggerFactory.getLogger(TypeConversionFormatFactory.class).error("Unable to instantiate converter: " + converter.getName(), ex);
        }
    }

    public static FormatConverter getConverter(String type) {
        return ConverterMap.getConverter(type);
    }
}

final class ConverterMap {

    private static final Map<String, FormatConverter> CONVERSION_MAP = new HashMap<>();

    private ConverterMap() {
    }

    public static void addConverter(String name, FormatConverter converter) {
        CONVERSION_MAP.put(name, converter);
    }

    public static FormatConverter getConverter(String name) {
        if (StringUtilities.isBlank(name)) {
            return null;
        }

        return CONVERSION_MAP.get(name);
    }
}
