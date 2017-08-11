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
package org.openrepose.filters.translation;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.filters.translation.config.*;
import org.springframework.mock.web.MockFilterConfig;

import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

public class TranslationFilterHandlersTest {
    private TranslationFilter filter;

    @Before
    public void setup() throws Exception {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        String configurationRoot = "";
        filter = new TranslationFilter(configurationService, configurationRoot);

        MockFilterConfig mockFilterConfig = new MockFilterConfig("TranslationFilter");
        filter.init(mockFilterConfig);
    }

    @Test
    public void shouldCreateProcessorPoolsOnConfigUpdate() throws Exception {
        TranslationConfig config = new TranslationConfig();
        RequestTranslations requestTranslations = new RequestTranslations();
        ResponseTranslations responseTranslations = new ResponseTranslations();

        RequestTranslation trans1 = new RequestTranslation();
        StyleSheets sheets = new StyleSheets();
        StyleSheet sheet = new StyleSheet();
        sheet.setId("sheet1");
        sheet.setHref("classpath:///style.xsl");
        sheets.getStyle().add(sheet);
        trans1.setAccept(APPLICATION_XML_VALUE);
        trans1.setContentType(APPLICATION_XML_VALUE);
        trans1.setTranslatedContentType(APPLICATION_XML_VALUE);
        trans1.setStyleSheets(sheets);

        requestTranslations.getRequestTranslation().add(trans1);

        ResponseTranslation trans2 = new ResponseTranslation();
        trans2.setAccept(APPLICATION_XML_VALUE);
        trans2.setContentType(APPLICATION_XML_VALUE);
        trans2.setCodeRegex("4[\\d]{2}");
        trans2.setTranslatedContentType(APPLICATION_XML_VALUE);
        trans2.setStyleSheets(sheets);

        responseTranslations.getResponseTranslation().add(trans2);

        config.setRequestTranslations(requestTranslations);
        config.setResponseTranslations(responseTranslations);
        filter.configurationUpdated(config);
//        assertEquals(1, filter.getRequestProcessors().size());
//        assertEquals(1, filter.getResponseProcessors().size());
    }
}
