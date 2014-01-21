package com.rackspace.papi.components.datastore.impl.ehcache;

import com.rackspace.papi.components.datastore.Patchable;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class EHCacheDatastoreTest {

    private static CacheManager cacheManager;
    private Cache cache;
    private EHCacheDatastore datastore;
    private static final String CACHE_NAME = "TEST";

    @BeforeClass
    public static void setUpClass() {
        Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setName("TestCacheManager");
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        defaultConfiguration.setUpdateCheck(false);

        cacheManager = CacheManager.newInstance(defaultConfiguration);
    }

    @AfterClass
    public static void tearDownClass() {
        cacheManager.removalAll();
        cacheManager.shutdown();
    }

    @Before
    public void setUp() {
        cache = new Cache(UUID.randomUUID().toString(), 20000, false, false, 5, 2);
        cacheManager.addCache(cache);
        datastore = new EHCacheDatastore(cache);
    }

    @Test
    public void get_getsNullElement() {
        final String key = "doesn't exist";
        Serializable element = datastore.get(key);
        assertNull(element);
    }

    @Test
    public void put_getsExistingElement() {
        final String key = "my element";
        String value = "1, 2, 3";

        datastore.put(key, value);

        Serializable element = datastore.get(key);
        assertNotNull(element);
        assertThat((String) element, equalTo(value));
    }

    @Test
    public void remove_removesExistingElement() {
        final String key = "my other element";
        String value = "1, 2, 3";

        datastore.put(key, value);
        datastore.remove(key);

        Serializable element = datastore.get(key);
        assertNull(element);
    }

    @Test
    public void put_removesExistingElementPastTTL() throws Exception {
        final String key = "my other element";
        String value = "1, 2, 3";
        int ttl = 10;

        datastore.put(key, value, ttl, MILLISECONDS);

        Serializable element = datastore.get(key);
        assertNotNull(element);
        assertThat((String) element, equalTo(value));

        Thread.sleep(10);

        element = datastore.get(key);
        assertNull(element);
    }

    @Test
    public void removeAll_removesAllEntries() throws Exception {
        String key1 = "a key";
        String key2 = "a different key";
        String value = "some value";

        datastore.put(key1, value);
        datastore.put(key2, value);

        Serializable element = datastore.get(key1);
        assertNotNull(element);
        element = datastore.get(key2);
        assertNotNull(element);

        datastore.removeAll();

        element = datastore.get(key1);
        assertNull(element);
        element = datastore.get(key2);
        assertNull(element);
    }

    @Test
    public void getName_getName() throws Exception {
        assertThat(datastore.getName(), equalTo("local/default"));
    }

    @Test
    public void shouldPatchNewElement() {
        String key = "my element";
        String value = "1, 2, 3";
        datastore.patch(key, new TestValue.Patch(value));
        TestValue element = (TestValue) datastore.get(key);
        assertNotNull(element);
        assertEquals(value, element.getValue());
    }

    @Test
    public void shouldPatchNewElementWithTTL(){
        String key = "my element";
        String value = "1, 2, 3";
        datastore.patch(key, new TestValue.Patch(value), 5, DAYS);
        TestValue element = (TestValue)datastore.get(key);
        assertNotNull(element);
        assertEquals(value, element.getValue());
    }

    @Test
    public void shouldPatchExistingElement(){
        String key = "my element";
        String value = "1, 2, 3";
        String newValue = ", 4";
        datastore.patch(key, new TestValue.Patch(value));
        datastore.patch(key, new TestValue.Patch(newValue));
        TestValue element = (TestValue)datastore.get(key);
        assertNotNull(element);
        assertEquals("1, 2, 3, 4", element.getValue());
    }

    @Test
    public void shouldPatchExistingElementWithTTL(){
        String key = "my element";
        String value = "1, 2, 3";
        String newValue = ", 4";
        datastore.patch(key, new TestValue.Patch(value), 5, DAYS);
        datastore.patch(key, new TestValue.Patch(newValue), 5, DAYS);
        TestValue element = (TestValue)datastore.get(key);
        assertNotNull(element);
        assertEquals("1, 2, 3, 4", element.getValue());
    }

    @Test
    public void patch_shouldReturnUpdatedValue() throws Exception {
        String key = "my element";
        String value = "1, 2, 3";
        String newValue = ", 4";
        datastore.patch(key, new TestValue.Patch(value), 5, DAYS);
        TestValue element = (TestValue)datastore.patch(key, new TestValue.Patch(newValue), 5, DAYS);
        assertNotNull(element);
        assertEquals("1, 2, 3, 4", element.getValue());
    }

    public static class TestValue implements Patchable<TestValue, TestValue.Patch>, Serializable {
        private String value;

        public TestValue(String value) {
            this.value = value;
        }

        @Override
        public TestValue applyPatch(Patch patch) {
            String originalValue = value;
            value = value + patch.newFromPatch().getValue();
            return new TestValue(originalValue + patch.newFromPatch().getValue());
        }

        public String getValue() {
            return value;
        }

        public static class Patch implements com.rackspace.papi.components.datastore.Patch<TestValue> {
            private String value;

            public Patch(String value) {
                this.value = value;
            }

            @Override
            public TestValue newFromPatch() {
                return new TestValue(value);
            }
        }
    }
}
