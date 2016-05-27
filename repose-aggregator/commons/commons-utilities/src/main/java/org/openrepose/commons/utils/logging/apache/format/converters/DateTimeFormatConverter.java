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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DateTimeFormatConverter implements FormatConverter {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DateTimeFormatConverter.class);

    @Override
    public String convert(String value, String inputFormat, String outputFormat) {

        if (!StringUtilities.isBlank(value) && !StringUtilities.isBlank(outputFormat)) {
            try {
                String inputPattern = DateConversionFormat.getPattern(inputFormat);
                String outputPattern = DateConversionFormat.getPattern(outputFormat);
                SimpleDateFormat format = new SimpleDateFormat(outputPattern);
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                return format.format(new SimpleDateFormat(inputPattern).parse(value));
            } catch (ParseException ex) {
                LOG.warn("Invalid date conversion parameters: " + value + "/" + inputFormat + "/" + outputFormat, ex);
            }
        }

        return value;
    }
}
