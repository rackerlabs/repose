package com.rackspace.papi.service.naming;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.naming.*;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@SuppressWarnings("UseOfObsoleteCollectionType")
@RunWith(Enclosed.class)
public class PowerApiNamingContextTest {

    private static final String LOCAL_REF = "powerapi:/object", SUBCONTEXT = "subcontext", SUBCONTEXT_REF = "powerapi:/subcontext/object";

    @Ignore("Testing Context Class - Ignored")
    public static class TestContext {

        protected Context context;

        @Before
        public void standUp() {
            context = new PowerApiNamingContext("", new Hashtable());
        }
    }

    public static class WhenUsingSubcontexts extends TestContext {

        @Test
        public void shouldCreateSubcontexts() throws Exception {
            final Object expected = new Object();

            context.createSubcontext(SUBCONTEXT);
            context.bind(SUBCONTEXT_REF, expected);

            assertEquals("Bound object to naming path in subcontext must match expected", context.lookup(SUBCONTEXT_REF), expected);
        }

        @Test(expected = NameNotFoundException.class)
        public void shouldFailToDestroyNonExistantSubcontext() throws Exception {
            context.destroySubcontext(SUBCONTEXT);
        }
        
        @Test(expected = NameNotFoundException.class)
        public void shouldDestroyLocalSubcontexts() throws Exception {
            final Object expected = new Object();

            try {
                context.createSubcontext(SUBCONTEXT);
                context.bind(SUBCONTEXT_REF, expected);
                context.destroySubcontext(SUBCONTEXT);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex.getCause());
            }

            context.lookup(SUBCONTEXT_REF);
        }

        @Test(expected = NameNotFoundException.class)
        public void shouldDestroyDeepSubcontexts() throws Exception {
            final String firstSubcontext = "subcontext_a", fullPath = "subcontext_a/" + SUBCONTEXT;
            final Object expected = new Object();

            try {
                context.createSubcontext(firstSubcontext).createSubcontext(SUBCONTEXT);
                context.bind(SUBCONTEXT_REF, expected);
                context.lookup(fullPath);
                context.destroySubcontext(fullPath);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex.getCause());
            }

            context.lookup(fullPath);
        }
    }

    public static class WhenParsingNames extends TestContext {

        @Test(expected = InvalidNameException.class)
        public void shouldOnlySupportPowerApiScheme() throws Exception {
            context.getNameParser("http://test.com/test");
        }

        @Test
        public void shouldParsePowerApiScheme() throws Exception {
            final Name name = context.getNameParser(SUBCONTEXT_REF).parse(SUBCONTEXT_REF);

            assertFalse("Name must not be null", name.isEmpty());
            assertEquals("First part of complex context name should be subcontext", "subcontext", name.get(0));
            assertEquals("Second part of complex context name should be object", "object", name.get(1));
        }
    }

    public static class WhenBinding extends TestContext {

        @Test(expected = InvalidNameException.class)
        public void shouldNotBindNullStringNames() throws Exception {
            final String nullString = null;

            context.bind(nullString, new Object());
        }

        @Test(expected = InvalidNameException.class)
        public void shouldNotBindNullNames() throws Exception {
            final Name nullName = null;

            context.bind(nullName, new Object());
        }

        @Test(expected = InvalidNameException.class)
        public void shouldNotBindEmptyNames() throws Exception {
            final Name nullName = new CompositeName("");

            context.bind(nullName, new Object());
        }

        @Test(expected = InvalidNameException.class)
        public void shouldNotBindEmptyStrings() throws Exception {
            context.bind("", new Object());
        }

        @Test(expected = InvalidNameException.class)
        public void shouldNotBindBlankNames() throws Exception {
            context.bind("    \t\t\t\r\n", new Object());
        }

        @Test
        public void shouldBindLocalEntites() throws Exception {
            final Object expected = new Object();

            context.bind(LOCAL_REF, expected);

            assertEquals("Bound object to naming path /object must match expected", context.lookup(LOCAL_REF), expected);
        }

        @Test(expected = NameAlreadyBoundException.class)
        public void shouldNotBindAlreadyBoundLocalEntites() throws Exception {
            final Object expected = new Object();

            context.bind(LOCAL_REF, expected);
            context.bind(LOCAL_REF, expected);
        }
    }

    public static class WhenUnbinding extends TestContext {

        @Test(expected = NameNotFoundException.class)
        public void shouldFailToUnbindNonExistantEntries() throws Exception {
            context.unbind(LOCAL_REF);
        }

        @Test(expected = NameNotFoundException.class)
        public void shouldUnbindLocalEntries() throws Exception {
            final Object expected = new Object();

            try {
                context.bind(LOCAL_REF, expected);
                context.unbind(LOCAL_REF);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex.getCause());
            }

            context.lookup(LOCAL_REF);
        }

        @Test(expected = NameNotFoundException.class)
        public void shouldUnbindDeepEntries() throws Exception {
            final Object expected = new Object();

            try {
                context.createSubcontext(SUBCONTEXT);
                context.bind(SUBCONTEXT_REF, expected);
                context.unbind(SUBCONTEXT_REF);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex.getCause());
            }

            context.lookup(SUBCONTEXT_REF);
        }
    }

    public static class WhenRebinding extends TestContext {

        @Test(expected = NameNotFoundException.class)
        public void shouldFailToRebindUnboundEntries() throws Exception {
            final Object expected = new Object();

            context.rebind(LOCAL_REF, expected);
        }

        @Test
        public void shouldRebindLocalEntries() throws Exception {
            final Object expected = new Object();

            try {
                context.bind(LOCAL_REF, new Object());
                context.rebind(LOCAL_REF, expected);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex.getCause());
            }

            assertEquals("Rebound entry must match expected", expected, context.lookup(LOCAL_REF));
        }

        @Test
        public void shouldRebindDeepEntries() throws Exception {
            final Object expected = new Object();

            try {
                context.createSubcontext(SUBCONTEXT);
                context.bind(SUBCONTEXT_REF, new Object());
                context.rebind(SUBCONTEXT_REF, expected);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex.getCause());
            }

            assertEquals("Rebound entry must match expected", expected, context.lookup(SUBCONTEXT_REF));
        }
    }
}
