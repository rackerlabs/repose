package com.rackspace.papi.components.cnorm.normalizer;

import com.rackspace.papi.components.normalization.config.MediaType;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class MediaTypeNormalizerTest {

    public static class WhenNormalizingVariantExtensions {

        private List<MediaType> configuredMediaTypes;
        private MediaTypeNormalizer normalizer;
        
        @Before
        public void standUp() {
            configuredMediaTypes = new LinkedList<MediaType>();

            final MediaType configuredMediaType = new MediaType();
            configuredMediaType.setName("application/xml");
            configuredMediaType.setVariantExtension("xml");
            configuredMediaType.setPreferred(Boolean.TRUE);

            configuredMediaTypes.add(configuredMediaType);
            
            normalizer = new MediaTypeNormalizer(configuredMediaTypes);
        }

        @Test
        public void shouldCorrectlyCaptureVariantExtensions() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri.xml"));
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
            assertEquals("/a/request/uri", director.getRequestUri());
            assertEquals("http://localhost/a/request/uri", director.getRequestUrl().toString());
        }

        @Test
        public void shouldCorrectlyIgnoreQueryParameters() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml?name=name&value=1");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri.xml?name=name&value=1"));
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
            assertEquals("/a/request/uri?name=name&value=1", director.getRequestUri());
            assertEquals("http://localhost/a/request/uri?name=name&value=1", director.getRequestUrl().toString());
        }

        @Test
        public void shouldCorrectlyIgnoreUriFragments() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml#fragment");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri.xml#fragment"));
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
            assertEquals("/a/request/uri#fragment", director.getRequestUri());
            assertEquals("http://localhost/a/request/uri#fragment", director.getRequestUrl().toString());
        }

        @Test
        public void shouldCorrectlyIgnoreUriFragmentsAndQueryParameters() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml?name=name&value=1#fragment");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri.xml?name=name&value=1#fragment"));
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
            assertEquals("/a/request/uri?name=name&value=1#fragment", director.getRequestUri());
            assertEquals("http://localhost/a/request/uri?name=name&value=1#fragment", director.getRequestUrl().toString());
        }

        @Test
        public void shouldCorrectlyCaptureUnusualVariantExtensions() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getRequestURI()).thenReturn("/a/request/uri/.xml");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri/.xml"));
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
            assertEquals("/a/request/uri/", director.getRequestUri());
            assertEquals("http://localhost/a/request/uri/", director.getRequestUrl().toString());
        }

        @Test
        public void should() {
            Pattern VARIANT_EXTRACTOR_REGEX = Pattern.compile("((\\.)[^\\d][\\w]*)");

            Matcher variantMatcher = VARIANT_EXTRACTOR_REGEX.matcher("http://localhost:8080/v1/test-service-mock-0.9.2-SNAPSHOT/whatever.xml");

            if (variantMatcher.find()) {
                for (int i = 1; i <=  variantMatcher.groupCount(); i++) {
                    System.out.println(variantMatcher.group(i));
                }
            }
        }
    }

    /**
     * Test of normalizeContentMediaType method, of class MediaTypeNormalizer.
     */
    @Test
    public void testNormalizeContentMediaType() {
        System.out.println("normalizeContentMediaType");
        HttpServletRequest request = null;
        FilterDirector director = null;
        MediaTypeNormalizer instance = null;
        instance.normalizeContentMediaType(request, director);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMediaTypeForVariant method, of class MediaTypeNormalizer.
     */
    @Test
    public void testGetMediaTypeForVariant() {
        System.out.println("getMediaTypeForVariant");
        HttpServletRequest request = null;
        FilterDirector director = null;
        MediaTypeNormalizer instance = null;
        MediaType expResult = null;
        MediaType result = instance.getMediaTypeForVariant(request, director);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of formatVariant method, of class MediaTypeNormalizer.
     */
    @Test
    public void testFormatVariant() {
        System.out.println("formatVariant");
        String variant = "";
        String expResult = "";
        String result = MediaTypeNormalizer.formatVariant(variant);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
