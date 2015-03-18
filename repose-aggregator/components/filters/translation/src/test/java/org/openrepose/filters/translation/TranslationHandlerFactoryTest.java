/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.filters.translation;

import org.openrepose.filters.translation.config.*;
import org.openrepose.core.services.config.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class TranslationHandlerFactoryTest {

    public static class WhenBuildingHandlers {
        
        private TranslationHandlerFactory factory;
        private String xml = "application/xml";
        private ConfigurationService manager;

        @Before
        public void setUp() {
            manager = mock(ConfigurationService.class);
            factory = new TranslationHandlerFactory(manager, "", "");
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
            trans1.setAccept(xml);
            trans1.setContentType(xml);
            trans1.setTranslatedContentType(xml);
            trans1.setStyleSheets(sheets);
            
            requestTranslations.getRequestTranslation().add(trans1);

            ResponseTranslation trans2 = new ResponseTranslation();
            trans2.setAccept(xml);
            trans2.setContentType(xml);
            trans2.setCodeRegex("4[\\d]{2}");
            trans2.setTranslatedContentType(xml);
            trans2.setStyleSheets(sheets);
            
            responseTranslations.getResponseTranslation().add(trans2);
            
            config.setRequestTranslations(requestTranslations);
            config.setResponseTranslations(responseTranslations);
            factory.configurationUpdated(config);
            TranslationHandler handler = factory.buildHandler();
            assertNotNull(handler);
            assertEquals(1, handler.getRequestProcessors().size());
            assertEquals(1, handler.getResponseProcessors().size());
        }
    }
}
