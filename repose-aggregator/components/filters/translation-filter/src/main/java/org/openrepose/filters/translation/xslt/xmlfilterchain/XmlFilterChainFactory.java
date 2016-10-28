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
package org.openrepose.filters.translation.xslt.xmlfilterchain;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.filters.translation.config.StyleSheet;
import org.openrepose.filters.translation.config.TranslationBase;
import org.openrepose.filters.translation.xslt.StyleSheetInfo;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public class XmlFilterChainFactory extends BasePoolableObjectFactory<XmlFilterChain> {

    private final XmlFilterChainBuilder builder;
    private final TranslationBase translation;
    private final String configRoot;
    private final String config;

    public XmlFilterChainFactory(final XmlFilterChainBuilder xsltChainBuilder, final TranslationBase translation, final String configRoot, final String config) {
        this.builder = xsltChainBuilder;
        this.translation = translation;
        this.configRoot = configRoot;
        this.config = config;
    }

    private String getAbsoluteXslPath(String xslPath) {
        return !xslPath.contains("://") ? StringUtilities.join("file://", configRoot, "/", xslPath) : xslPath;
    }

    @Override
    public XmlFilterChain makeObject() {
        List<StyleSheetInfo> stylesheets = new ArrayList<>();
        if (translation.getStyleSheets() != null) {
            for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                if (sheet.getXsl() != null && sheet.getXsl().getAny() != null) {
                    stylesheets.add(new StyleSheetInfo(sheet.getId(), (Node) sheet.getXsl().getAny(), getAbsoluteXslPath(config)));
                } else {
                    stylesheets.add(new StyleSheetInfo(sheet.getId(), getAbsoluteXslPath(sheet.getHref())));
                }
            }
        }

        return builder.build(stylesheets.toArray(new StyleSheetInfo[0]));
    }
}
