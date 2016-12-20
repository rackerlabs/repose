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
package org.openrepose.commons.utils.http.media;

import org.openrepose.commons.utils.StringUtilities;

public enum MimeType {

    APPLICATION_ATOM_XML(MimeType.APPLICATION, "atom+xml"),
    APPLICATION_RDF_XML(MimeType.APPLICATION, "rdf+xml"),
    APPLICATION_RSS_XML(MimeType.APPLICATION, "rss+xml"),
    APPLICATION_SOAP_XML(MimeType.APPLICATION, "soap+xml"),
    APPLICATION_XHTML_XML(MimeType.APPLICATION, "xhtml+xml"),
    APPLICATION_XML_DTD(MimeType.APPLICATION, "xml-dtd"),
    APPLICATION_XOP_XML(MimeType.APPLICATION, "xop+xml"),
    APPLICATION_XML(MimeType.APPLICATION, "xml"),
    APPLICATION_JSON(MimeType.APPLICATION, "json"),
    TEXT_HTML("text", "html"),
    TEXT_PLAIN("text", "plain"),
    TEXT_XML("text", "xml"),
    IMAGE_SVG_XML("image", "svg+xml"),
    MESSAGE_IMDN_XML("message", "imdn+xml"),
    MODEL_X3D_XML("model", "x3d+xml"),
    WILDCARD("*", "*"),
    UNKNOWN("", ""),
    UNSPECIFIED("", "");

    private static final String APPLICATION = "application";
    private final String topLevelTypeName;
    private final String subTypeName;
    private final String name;

    MimeType(String topLevelTypeName, String subTypeName) {
        this.topLevelTypeName = topLevelTypeName;
        this.subTypeName = subTypeName;
        this.name = topLevelTypeName + "/" + subTypeName;
    }

    public static MimeType getMatchingMimeType(String mimeType) {
        if (StringUtilities.isNotBlank(mimeType)) {
            for (MimeType ct : values()) {
                if (ct.getName().equalsIgnoreCase(mimeType)) {
                    return ct;
                }
            }
        }
        return UNKNOWN;
    }

    public static MimeType guessMediaTypeFromString(String mimeType) {
        if (StringUtilities.isNotBlank(mimeType)) {

            for (MimeType ct : values()) {
                // worst case scenario this will match on UNKNOWN because everything contains "" (an empty string)
                if (mimeType.contains(ct.getTopLevelTypeName()) && mimeType.contains(ct.getSubTypeName())) {
                    return ct;
                }
            }

            // this is unreachable code, and at this point I'm too afraid to fix it
            for (MimeType ct : values()) {
                if (mimeType.contains(ct.getSubTypeName())) {
                    return ct;
                }
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getSubTypeName() {
        return subTypeName;
    }

    public String getTopLevelTypeName() {
        return topLevelTypeName;
    }
}
