package com.rackspace.papi.components.datastore.impl.ehcache;

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
import java.util.concurrent.TimeUnit;

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
        assertThat((String)element, equalTo(value));
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

        datastore.put(key, value, ttl, TimeUnit.MILLISECONDS);

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
    public void shouldPatchNewElement(){
        String key = "my element";
        byte[] value = { 1, 2, 3};
        datastore.patch(key, value);
        StoredElement element = datastore.get(key);
        assertNotNull(element);
        assertEquals(value, element.elementBytes());
    }

    @Test
    public void shouldPatchNewElementWithTTL(){
        String key = "my element";
        byte[] value = { 1, 2, 3};
        datastore.patch(key, value, 5, TimeUnit.DAYS);
        StoredElement element = datastore.get(key);
        assertNotNull(element);
        assertEquals(value, element.elementBytes());
    }

    @Test
    public void shouldPatchExistingElement(){
        String key = "my element";
        byte[] value = { 1, 2, 3};
        byte[] newValue = { 4 };
        datastore.patch(key, value);
        datastore.patch(key, newValue);
        StoredElement element = datastore.get(key);
        assertNotNull(element);
        assertEquals(new byte[] {1,2,3,4}, element.elementBytes());
    }

    @Test
    public void shouldPatchExistingElementWithTTL(){
        String key = "my element";
        byte[] value = { 1, 2, 3};
        byte[] newValue = { 4 };
        datastore.patch(key, value, 5, TimeUnit.DAYS);
        datastore.patch(key, newValue, 5, TimeUnit.DAYS);
        StoredElement element = datastore.get(key);
        assertNotNull(element);
        assertEquals(new byte[] {1,2,3,4}, element.elementBytes());
    }
}
