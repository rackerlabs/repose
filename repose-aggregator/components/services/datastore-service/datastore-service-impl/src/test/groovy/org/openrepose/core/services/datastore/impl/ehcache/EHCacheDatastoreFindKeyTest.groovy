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
package org.openrepose.core.services.datastore.impl.ehcache

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by adrian on 7/6/17.
 */
class EHCacheDatastoreFindKeyTest extends Specification {
    static CacheManager cacheManager
    Cache cache
    EHCacheDatastore datastore

    def setupSpec() {
        Configuration defaultConfiguration = new Configuration()
        defaultConfiguration.setName("TestCacheManager")
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false))
        defaultConfiguration.setUpdateCheck(false)

        cacheManager = CacheManager.newInstance(defaultConfiguration)
    }

    def cleanupSpec() {
        cacheManager.removalAll()
        cacheManager.shutdown()
    }

    def setup() {
        cache = new Cache(UUID.randomUUID().toString(), 20000, false, false, 5, 2)
        cacheManager.addCache(cache)
        datastore = new EHCacheDatastore(cache)
    }

    @Unroll
    def "findKeys returns the right keys for #criteria"() {
        given:
        populateDatastore()

        when:
        List<String> returnedKeys = datastore.findKeys(criteria)

        then:
        returnedKeys.size() == expectedKeys.size()
        returnedKeys.containsAll(expectedKeys)

        where:
        criteria | expectedKeys
        "abc?"   | ["abcd", "abc1"]
        "abc*"   | ["abcd", "abc1", "abcdef"]
        "?bcd"   | ["abcd", "1bcd"]
        "*bcd"   | ["abcd", "1bcd", "aabcd"]
        "?bc?"   | ["abcd", "abc1", "1bcd"]
        "?bc*"   | ["abcd", "abc1", "1bcd", "abcdef"]
        "*bc?"   | ["abcd", "abc1", "1bcd", "aabcd"]
        "a?cd"   | ["abcd"]
        "a*cd"   | ["abcd", "aabcd"]
    }

    def populateDatastore() {
        datastore.put("abcd", "banana");
        datastore.put("abc1", "banana");
        datastore.put("abcdef", "banana");
        datastore.put("1bcd", "banana");
        datastore.put("aabcd", "banana");
    }
}
