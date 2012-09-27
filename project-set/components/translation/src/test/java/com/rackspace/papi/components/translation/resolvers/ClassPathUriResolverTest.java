package com.rackspace.papi.components.translation.resolvers;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ClassPathUriResolverTest {

    public static class WhenResolveResources {
        private URIResolver parent;
        private ClassPathUriResolver resolver;

        @Before
        public void setUp() {
            parent = mock(URIResolver.class);
            resolver = new ClassPathUriResolver(parent);
        }
        
        @Test
        public void shouldFindResource() throws TransformerException {
            Source resource = resolver.resolve(ClassPathUriResolver.CLASSPATH_PREFIX + "/style.xsl", "");
            assertNotNull("Should find resource", resource);
        }
        
        @Test
        public void shouldReturnNullWhenResourceNotFound() throws TransformerException {
            Source resource = resolver.resolve(ClassPathUriResolver.CLASSPATH_PREFIX + "/blah.xsl", "");
            assertNull("Should return null for non-existent resource", resource);
        }
        
        @Test
        public void shouldHandleNullHref() throws TransformerException {
            Source resource = resolver.resolve(null, "");
            assertNull("Should handle null href", resource);
        }
        
        @Test
        public void shouldCallParentResolverForNonClassPathResources() throws TransformerException {
            String href = "/style.xsl";
            String base = "base";
            Source resource = resolver.resolve(href, base);
            verify(parent).resolve(eq(href), eq(base));
        }
    }
}
