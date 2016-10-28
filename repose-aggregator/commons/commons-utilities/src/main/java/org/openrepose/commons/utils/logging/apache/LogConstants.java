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
package org.openrepose.commons.utils.logging.apache;

import java.util.regex.Pattern;

/**
 * Created by adrian on 10/28/16.
 */
public class LogConstants {

    // Group 1
    public static final String LIFECYCLE_MODIFIER_EXTRACTOR = "([<>])?";
    // Group 2, 3 (ignore)
    public static final String STATUS_CODE_EXTRACTOR = "([!]?([0-9]{3}[,]?)*)?";
    // Group 4 (ignore), 5, 6, 7
    public static final String VARIABLE_EXTRACTOR = "(\\{([\\-a-zA-Z0-9:.]*)([ ,]?)([_\\-a-zA-Z0-9 ,:.]*)\\})?";
    // Group 8
    public static final String ENTITY_EXTRACTOR = "([%a-zA-Z])";
    public static final Pattern PATTERN = Pattern.compile("%" + LIFECYCLE_MODIFIER_EXTRACTOR + STATUS_CODE_EXTRACTOR + VARIABLE_EXTRACTOR + ENTITY_EXTRACTOR);
    public static final int LIFECYCLE_GROUP_INDEX = 1;
    public static final int STATUS_CODE_INDEX = 2;
    public static final int VARIABLE_INDEX = 5;
    public static final int VAR_ARG_SEPARATOR_INDEX = 6;
    public static final int ARGUMENTS_INDEX = 7;
    public static final int ENTITY_INDEX = 8;
}
