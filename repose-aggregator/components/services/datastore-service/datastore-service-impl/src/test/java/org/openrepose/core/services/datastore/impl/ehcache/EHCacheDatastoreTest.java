/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.datastore.impl.ehcache;

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
import org.openrepose.core.services.datastore.types.StringPatch;

import java.io.Serializable;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EHCacheDatastoreTest {

    private static CacheManager cacheManager;
    private Cache cache;
    private EHCacheDatastore datastore;

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
        assertThat(element, equalTo(value));
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
        int ttl = 1;

        datastore.put(key, value, ttl, SECONDS);

        Serializable element = datastore.get(key);
        assertNotNull(element);
        assertThat((String) element, equalTo(value));

        //todo: replace with within style matcher
        Thread.sleep(2000);

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
        datastore.patch(key, new StringPatch(value));
        String element = (String) datastore.get(key);
        assertNotNull(element);
        assertEquals(value, element);
    }

    @Test
    public void shouldPatchNewElementWithTTL() {
        String key = "my element";
        String value = "1, 2, 3";
        datastore.patch(key, new StringPatch(value), 5, DAYS);
        String element = (String) datastore.get(key);
        assertNotNull(element);
        assertEquals(value, element);
    }

    @Test
    public void shouldPatchExistingElement() {
        String key = "my element";
        String value = "1, 2, 3";
        String newValue = ", 4";
        datastore.patch(key, new StringPatch(value));
        datastore.patch(key, new StringPatch(newValue));
        String element = (String) datastore.get(key);
        assertNotNull(element);
        assertEquals("1, 2, 3, 4", element);
    }

    @Test
    public void shouldPatchExistingElementWithTTL() {
        String key = "my element";
        String value = "1, 2, 3";
        String newValue = ", 4";
        datastore.patch(key, new StringPatch(value), 5, DAYS);
        datastore.patch(key, new StringPatch(newValue), 5, DAYS);
        String element = (String) datastore.get(key);
        assertNotNull(element);
        assertEquals("1, 2, 3, 4", element);
    }

    @Test
    public void patch_shouldReturnUpdatedValue() throws Exception {
        String key = "my element";
        String value = "1, 2, 3";
        String newValue = ", 4";
        datastore.patch(key, new StringPatch(value), 5, DAYS);
        String element = datastore.patch(key, new StringPatch(newValue), 5, DAYS);
        assertNotNull(element);
        assertEquals("1, 2, 3, 4", element);
    }

    @Test
    public void patch_shouldSetTtl() throws Exception {
        Ehcache cache = mock(Ehcache.class);
        EHCacheDatastore datastore = new EHCacheDatastore(cache);
        ArgumentCaptor<Element> captor = ArgumentCaptor.forClass(Element.class);
        datastore.patch("key", new StringPatch("some value"), 10, SECONDS);
        verify(cache).putIfAbsent(captor.capture());
        assertThat(captor.getValue().getTimeToLive(), equalTo(10));
    }

    @Test
    public void patch_shouldResetTtl() throws Exception {
        Ehcache cache = mock(Ehcache.class);
        Element returnedElement = new Element("key", "");
        when(cache.putIfAbsent(any(Element.class))).thenReturn(returnedElement);
        when(cache.replace(any(Element.class), any(Element.class))).thenReturn(true);
        EHCacheDatastore datastore = new EHCacheDatastore(cache);
        ArgumentCaptor<Element> captor = ArgumentCaptor.forClass(Element.class);
        datastore.patch("key", new StringPatch("some value"), 10, SECONDS);
        verify(cache).replace(any(Element.class), captor.capture());
        assertThat(captor.getValue().getTimeToLive(), equalTo(10));
    }
}
