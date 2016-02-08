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

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public enum MimeType {

    APPLICATION_ATOM_XML("application", "atom+xml"),
    APPLICATION_RDF_XML("application", "rdf+xml"),
    APPLICATION_RSS_XML("application", "rss+xml"),
    APPLICATION_SOAP_XML("application", "soap+xml"),
    APPLICATION_XHTML_XML("application", "xhtml+xml"),
    APPLICATION_XML_DTD("application", "xml-dtd"),
    APPLICATION_XOP_XML("application", "xop+xml"),
    APPLICATION_XML("application", "xml"),
    APPLICATION_JSON("application", "json"),
    TEXT_HTML("text", "html"),
    TEXT_PLAIN("text", "plain"),
    TEXT_XML("text", "xml"),
    IMAGE_SVG_XML("image", "svg+xml"),
    MESSAGE_IMDN_XML("message", "imdn+xml"),
    MODEL_X3D_XML("model", "x3d+xml"),
    WILDCARD("*", "*"),
    UNKNOWN("", ""),
    UNSPECIFIED("", "");

    private final String type, subType, mimeType;

    MimeType(String type, String subType) {
        this.type = type;
        this.subType = subType;
        this.mimeType = type + "/" + subType;
    }

    public static MimeType getMatchingMimeType(String mimeType) {
        if (StringUtilities.isNotBlank(mimeType)) {
            for (MimeType ct : values()) {
                if (ct.getMimeType().equalsIgnoreCase(mimeType)) {
                    return ct;
                }
            }
        }
        return UNKNOWN;
    }

    public static MimeType guessMediaTypeFromString(String mimeType) {
        if (StringUtilities.isNotBlank(mimeType)) {

            for (MimeType ct : values()) {
                if (mimeType.contains(ct.getType()) && mimeType.contains(ct.getSubType())) {
                    return ct;
                }
            }

            for (MimeType ct : values()) {
                if (mimeType.contains(ct.getSubType())) {
                    return ct;
                }
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getSubType() {
        return subType;
    }

    public String getType() {
        return type;
    }
}
