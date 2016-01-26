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

import org.openrepose.commons.utils.string.JCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.openrepose.commons.utils.string.JCharSequenceFactory.jchars;

/**
 * This is a simple helper class that can be used to generalize URI related
 * string processing.
 *
 * @author zinic
 */
public final class StringUriUtilities {
    private static final Logger LOG = LoggerFactory.getLogger(StringUriUtilities.class);

    private StringUriUtilities() {
        // Empty constructor for utility class.
    }

    public static int indexOfUriFragment(String st, String uriFragment) {
        return indexOfUriFragment(jchars(st), uriFragment);
    }

    public static int indexOfUriFragment(JCharSequence uri, String uriFragment) {
        final int index = uri.indexOf(uriFragment);

        if (uri.length() > uriFragment.length() + index) {
            return uri.charAt(index + uriFragment.length()) == '/' ? index : -1;
        }

        return index;
    }

    public static String appendPath(String baseUrl, String... paths) {
        String path = concatUris(paths);
        if (StringUtilities.isBlank(path)) {
            return baseUrl;
        }

        StringBuilder url;
        if (baseUrl.endsWith("/")) {
            url = new StringBuilder(baseUrl.substring(0, baseUrl.length() - 1));
        } else {
            url = new StringBuilder(baseUrl);
        }

        return url.append(path).toString();
    }

    public static String concatUris(String... uris) {
        StringBuilder builder = new StringBuilder();

        for (String uri : uris) {
            if (StringUtilities.isNotBlank(uri)) {
                if (!uri.startsWith("/") && !uri.isEmpty()) {
                    builder.append("/");
                }
                builder.append(uri);
            }
        }

        return builder.toString();
    }

    /**
     * Formats a URI by adding a forward slash and removing the last forward
     * slash from the URI.
     * <p/>
     * e.g. some/random/uri/ -> /some/random/uri e.g. some/random/uri ->
     * /some/random/uri e.g. /some/random/uri/ -> /some/random/uri e.g. / -> /
     * e.g. ////// -> /
     *
     * @param uri
     * @return
     */
    public static String formatUri(String uri) {
        if (StringUtilities.nullSafeStartsWith(uri, "\\")) {
            //windows file system
            return uri;
        }

        if (StringUtilities.isBlank(uri) || StringUtilities.nullSafeEqualsIgnoreCase("/", uri)) {
            return "/";
        }

        final StringBuilder externalName = new StringBuilder(uri);

        if (externalName.charAt(0) != '/') {
            externalName.insert(0, "/");
        }

        int doubleSlash = externalName.indexOf("//");

        while (doubleSlash > -1) {
            //removes leading '/'
            externalName.replace(doubleSlash, doubleSlash + 2, "/");
            doubleSlash = externalName.indexOf("//");
        }


        if (externalName.charAt(externalName.length() - 1) == '/' && externalName.length() != 1) {
            externalName.deleteCharAt(externalName.length() - 1);
        }


        return externalName.toString();
    }

    public static String formatUriNoLead(String uri) {

        StringBuilder externalName = new StringBuilder(formatUri(uri));

        if (externalName.charAt(0) == '/') {
            externalName.deleteCharAt(0);
        }

        return externalName.toString();
    }

    public static String encodeUri(String uri) {

        String encodedUri = "";

        try {
            encodedUri = URLEncoder.encode(uri, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
            LOG.trace("failed to encode uri: " + uri, ignored);
        }

        return encodedUri;
    }
}
