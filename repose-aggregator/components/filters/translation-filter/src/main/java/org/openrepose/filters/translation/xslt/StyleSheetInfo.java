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
package org.openrepose.filters.translation.xslt;

import org.w3c.dom.Node;

public class StyleSheetInfo {
    private final String id;
    private final String uri;
    private final Node xsl;
    private final String systemId;

    public StyleSheetInfo(String id, String uri) {
        this.id = id;
        this.uri = uri;
        this.xsl = null;
        this.systemId = null;
    }

    public StyleSheetInfo(String id, Node xsl, String systemId) {
        this.id = id;
        this.uri = null;
        this.xsl = xsl;
        this.systemId = systemId;
    }

    public String getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public Node getXsl() {
        return xsl;
    }

    public String getSystemId() {
        return systemId;
    }
}
