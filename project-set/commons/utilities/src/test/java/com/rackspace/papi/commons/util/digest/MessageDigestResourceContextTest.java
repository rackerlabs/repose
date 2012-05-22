package com.rackspace.papi.commons.util.digest;

import com.rackspace.papi.commons.util.digest.impl.HexHelper;
import com.rackspace.papi.commons.util.digest.impl.MD5MessageDigester;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Apr 21, 2011
 * Time: 2:50:13 PM
 */
@RunWith(Enclosed.class)
public class MessageDigestResourceContextTest {
    private static final String SOURCE_DATA = "Lorem ipsum dolor sit amet";

    public static class WhenPerformingMessageDigest {
        private InputStream inputStream;
        private MessageDigestResourceContext context;
        

        @Before
        public void setup() throws IOException {
            inputStream = new ByteArrayInputStream(SOURCE_DATA.getBytes());

            context = new MessageDigestResourceContext(inputStream);
        }

        @Test
        public void shouldReturnNonNullValue() throws NoSuchAlgorithmException {
            String expected, actual;

            expected = "fea80f2db003d4ebc4536023814aa885";
            actual = HexHelper.convertToHex(new MD5MessageDigester().digestStream(inputStream));

            assertEquals(expected, actual);
        }

        @Test(expected=ResourceContextException.class)
        public void shouldThrowResourceContextExceptionIfIOErrorOccurs() throws IOException, NoSuchAlgorithmException {
            InputStream iStream = mock(InputStream.class);

            when(iStream.read(any(byte[].class)))
                    .thenThrow(new IOException("test"));

            context = new MessageDigestResourceContext(iStream);

            context.perform(MessageDigest.getInstance("MD5"));
        }
    }
}
