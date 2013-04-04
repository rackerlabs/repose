package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.service.datastore.StoredElement;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class EHCacheDatastoreTest {

    public static class WhenAccessingItems {
        private static CacheManager cacheManager;
        private Cache cache;
        private EHCacheDatastore instance;
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
            instance = new EHCacheDatastore(cache);
        }
        
        @Test
        public void shouldGetNullElement() {
            final String key = "doesn't exitst";
            StoredElement element = instance.get(key);
            assertNotNull(element);
            assertTrue(element instanceof StoredElement);
            assertTrue(element.elementIsNull());
        }
        
        @Test
        public void shouldGetExistingElement() {
            final String key = "my element";
            byte[] value = { 1, 2, 3};
            
            instance.put(key, value);
            
            StoredElement element = instance.get(key);
            assertNotNull(element);
            assertTrue(element instanceof StoredElement);
            assertFalse(element.elementIsNull());
            
        }

        @Test
        public void shouldRemoveExistingElement() {
            final String key = "my element";
            byte[] value = { 1, 2, 3};
            
            instance.put(key, value);
            instance.remove(key);
            
            StoredElement element = instance.get(key);
            assertNotNull(element);
            assertTrue(element instanceof StoredElement);
            assertTrue(element.elementIsNull());
        }
    }
}
