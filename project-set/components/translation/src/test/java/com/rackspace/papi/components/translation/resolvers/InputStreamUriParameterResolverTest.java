package com.rackspace.papi.components.translation.resolvers;

import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import java.io.InputStream;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class InputStreamUriParameterResolverTest {

    public static class WhenAddingStreams {
        private InputStreamUriParameterResolver parent;
        private InputStreamUriParameterResolver resolver;
        private InputStream input;

        @Before
        public void setUp() {
            parent = mock(InputStreamUriParameterResolver.class);
            resolver = new InputStreamUriParameterResolver(parent);
            input = mock(InputStream.class);
        }
        
        @Test
        public void shouldAddStream() throws TransformerException {
            String name = "data";
            String href = resolver.getHref(name);
            String actualHref = resolver.addStream(input, name);
            
            assertEquals("HREFs should be equal", href, actualHref);
            StreamSource source = (StreamSource) resolver.resolve(href, "base");
            assertNotNull("Shoudl find source stream", source);
            assertTrue("Streams should be the same", input == source.getInputStream());
        }
        
        @Test
        public void shouldCallParentResolver() throws TransformerException {
            String href = "otherdata";
            String base = "base";
            resolver.resolve(href, base);
            verify(parent).resolve(href, base);
        }
    }
}
