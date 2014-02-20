package com.rackspace.papi.components.datastore.impl.ehcache;

import com.rackspace.papi.components.datastore.StringValue;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.Serializable;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        Thread.sleep(20);

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
        datastore.patch(key, new StringValue.Patch(value));
        StringValue element = (StringValue) datastore.get(key);
        assertNotNull(element);
        assertEquals(value, element.getValue());
    }

    @Test
    public void shouldPatchNewElementWithTTL(){
        String key = "my element";
        String value = "1, 2, 3";
        datastore.patch(key, new StringValue.Patch(value), 5, DAYS);
        StringValue element = (StringValue)datastore.get(key);
        assertNotNull(element);
        assertEquals(value, element.getValue());
    }

    @Test
    public void shouldPatchExistingElement(){
        String key = "my element";
        String value = "1, 2, 3";
        String newValue = ", 4";
        datastore.patch(key, new StringValue.Patch(value));
        datastore.patch(key, new StringValue.Patch(newValue));
        StringValue element = (StringValue)datastore.get(key);
        assertNotNull(element);
        assertEquals("1, 2, 3, 4", element.getValue());
    }

    @Test
    public void shouldPatchExistingElementWithTTL(){
        String key = "my element";
        String value = "1, 2, 3";
        String newValue = ", 4";
        datastore.patch(key, new StringValue.Patch(value), 5, DAYS);
        datastore.patch(key, new StringValue.Patch(newValue), 5, DAYS);
        StringValue element = (StringValue)datastore.get(key);
        assertNotNull(element);
        assertEquals("1, 2, 3, 4", element.getValue());
    }

    @Test
    public void patch_shouldReturnUpdatedValue() throws Exception {
        String key = "my element";
        String value = "1, 2, 3";
        String newValue = ", 4";
        datastore.patch(key, new StringValue.Patch(value), 5, DAYS);
        StringValue element = (StringValue)datastore.patch(key, new StringValue.Patch(newValue), 5, DAYS);
        assertNotNull(element);
        assertEquals("1, 2, 3, 4", element.getValue());
    }

    @Test
    public void patch_shouldSetTtl() throws Exception {
        Ehcache cache = mock(Ehcache.class);
        EHCacheDatastore datastore = new EHCacheDatastore(cache);
        ArgumentCaptor<Element> captor = ArgumentCaptor.forClass(Element.class);
        datastore.patch("key", new StringValue.Patch("some value"), 10, SECONDS);
        verify(cache).putIfAbsent(captor.capture());
        assertThat(captor.getValue().getTimeToIdle(), equalTo(10));
    }

    @Test
    public void patch_shouldRaiseTtl_ifHigher() throws Exception {
        Ehcache cache = mock(Ehcache.class);
        Element returnedElement = new Element("key", new StringValue(""));
        when(cache.putIfAbsent(any(Element.class))).thenReturn(returnedElement);
        EHCacheDatastore datastore = new EHCacheDatastore(cache);
        datastore.patch("key", new StringValue.Patch("some value"), 10, SECONDS);
        assertThat(returnedElement.getTimeToIdle(), equalTo(10));
    }
}
