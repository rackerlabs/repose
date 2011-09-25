package com.rackspace.papi.components.cnorm.normalizer;

import com.rackspace.papi.components.normalization.config.MediaType;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml");
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
        }

        @Test
        public void shouldCorrectlyIgnoreQueryParameters() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml?name=name&value=1");
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
        }

        @Test
        public void shouldCorrectlyIgnoreUriFragments() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml#fragment");
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
        }

        @Test
        public void shouldCorrectlyIgnoreUriFragmentsAndQueryParameters() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml?name=name&value=1#fragment");
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
        }

        @Test
        public void shouldCorrectlyCaptureUnusualVariantExtensions() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            
            when(request.getRequestURI()).thenReturn("/a/request/uri/.xml");
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
        }
    }
}
