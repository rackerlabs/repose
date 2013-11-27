package org.openrepose.rackspace.auth_2_0.identity.parsers.xml;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class AuthenticationRequestParserTest {

    public static class TestParent {

        AuthenticationRequestParser authenticationRequestParser;
        Unmarshaller unmarshaller;
        InputStream inputStream;
        String content;

        @Before
        public void setUp() throws Exception {
            content = "content";
            inputStream = mock(InputStream.class);
            unmarshaller = mock(Unmarshaller.class);
            authenticationRequestParser = new AuthenticationRequestParser(unmarshaller);
        }

        @Test
        public void shouldReturnNullWhenParsingWithIncorrectString() {
            assertNull(authenticationRequestParser.parse(content));
        }

        @Test
        public void shouldReturnNullWhenParsingWithIncorrectInputStream() {
            assertNull(authenticationRequestParser.parse(inputStream));
        }
    }
}
