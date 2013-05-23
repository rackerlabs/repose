package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.TranslationHandlerFactory;
import com.rackspace.papi.components.translation.xslt.StyleSheetInfo;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class XsltFilterChainBuilderTest {
    public static class WhenBuildingChains {

        private static SAXTransformerFactory factory;
        private XmlFilterChainBuilder builder;

        @BeforeClass
        public static void before() {
            System.setProperty("javax.xml.transform.TransformerFactory", TranslationHandlerFactory.SAXON_HE_FACTORY_NAME);
            factory = (SAXTransformerFactory) TransformerFactory.newInstance();
        }

        @Before
        public void setUp() {
            builder = new XmlFilterChainBuilder(factory, false, true);
        }

        @Test
        public void shouldHandleEmptySetOfStyles() {
            XmlFilterChain chain = builder.build();

            assertNotNull("Should build an empty filter chain", chain);
            assertEquals("Should have 1 filter", 1, chain.getFilters().size());
        }

        @Test
        public void shouldHandleStyleSheetList() {
            XmlFilterChain chain = builder.build(new StyleSheetInfo("", "classpath:///style.xsl"));

            assertNotNull("Should build a filter chain", chain);
            assertEquals("Should have 1 filter", 1, chain.getFilters().size());
        }

    }
    
    public static class WhenExecutingChains {
        private static SAXTransformerFactory factory;
        private XmlFilterChainBuilder builder;
        private ByteArrayOutputStream output;
        private InputStream body;

        @BeforeClass
        public static void before() {
            System.setProperty("javax.xml.transform.TransformerFactory", TranslationHandlerFactory.SAXON_HE_FACTORY_NAME);
            factory = (SAXTransformerFactory) TransformerFactory.newInstance();
        }

        @Before
        public void setUp() {
            builder = new XmlFilterChainBuilder(factory, false, true);
            output = new ByteArrayOutputStream();
            body = getClass().getResourceAsStream("/empty.xml");
        }

        @Test
        public void shouldUseInputOutputStreams() {
            List<XsltParameter> inputs = new ArrayList<XsltParameter>();

            List<XsltParameter<? extends OutputStream>> outputs = new ArrayList<XsltParameter<? extends OutputStream>>();
            
            XmlFilterChain chain = builder.build(new StyleSheetInfo("", "classpath:///style.xsl"));
            chain.executeChain(body, output, inputs, null);
            
            String outResult = output.toString();
            
            assertTrue("Shoudl have main output", outResult.length() > 0);
            
        }
        
    }
    
}
