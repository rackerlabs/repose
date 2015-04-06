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
package org.openrepose.commons.utils;

import java.util.regex.Pattern;

/**
 * Local StringUtils implementation...this is written because we don't want a dependency on
 * apache commons...it's too big for what we need.
 */
public final class StringUtilities {

    private static final Pattern IS_BLANK_PATTERN = Pattern.compile("[\\s]*");

    private StringUtilities() {
    }

    public static boolean isEmpty(String st) {
        return st == null || st.length() == 0;
    }

    public static boolean isBlank(String st) {
        return isEmpty(st) || IS_BLANK_PATTERN.matcher(st).matches();
    }

    public static String trim(String st, String trim) {
        if (st.length() < trim.length()) {
            return st;
        }

        final String next = st.startsWith(trim) ? st.substring(trim.length()) : st;
        return next.endsWith(trim) ? next.substring(0, next.length() - trim.length()) : next;
    }

    public static String join(Object... args) {
        final StringBuilder builder = new StringBuilder();

        for (Object a : args) {
            builder.append(a);
        }

        return builder.toString();
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static String getValue(String string, String defaultValue) {
        if (string != null) {
            return string;
        }

        return defaultValue;
    }

    public static String getNonBlankValue(String string, String defaultValue) {
        if (!isBlank(string)) {
            return string;
        }

        return defaultValue;
    }

    public static boolean nullSafeEqualsIgnoreCase(String one, String two) {
        return one == null ? two == null : (two != null && one.equalsIgnoreCase(two));
    }

    public static boolean nullSafeEquals(String one, String two) {
        return one == null ? two == null : (two != null && one.equals(two));
    }

    public static boolean nullSafeStartsWith(String one, String two) {
        return !((one == null) || (two == null)) && one.startsWith(two);
    }
}
