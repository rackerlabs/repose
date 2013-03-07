package com.rackspace.papi.components.translation;

import com.rackspace.papi.components.translation.config.*;
import com.rackspace.papi.service.config.ConfigurationService;
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
        public void shouldCreateProcessorPoolsOnConfigUpdate() {
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
