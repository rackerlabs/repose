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
package org.openrepose.commons.config.manager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.thread.KeyedStackLock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 7/6/11
 * Time: 1:32 PM
 */
@RunWith(Enclosed.class)
public class LockedConfigurationUpdaterTest {
    public static class WhenLockingOperations {
        private KeyedStackLock updateLock;
        private Object updateKey1, updateKey2;
        private List<String> configProperties;

        @Before
        public void setup() {
            updateLock = new KeyedStackLock();
            updateKey1 = new Object();
            updateKey2 = new Object();
            configProperties = new ArrayList<String>();
        }

        @Test
        @Ignore
        public void shouldLockWhenUsingUniqueKeys() throws InterruptedException {
            final SampleConfigObject config = new SampleConfigObject(configProperties);
            final TestConfigUpdater updater1 = new TestConfigUpdater(updateLock, updateKey1, "prop1", 10);
            final TestConfigUpdater updater2 = new TestConfigUpdater(updateLock, updateKey2, "prop2", 0);
            Thread t1 = new Thread() {
                @Override
                public void run() {
                    updater1.configurationUpdated(config);
                }
            };
            Thread t2 = new Thread() {
                @Override
                public void run() {
                    updater2.configurationUpdated(config);
                }
            };

            t1.start();
            t2.start();

            //Thread.sleep(20);

            t1.join();
            t2.join();

            assertEquals("prop1", configProperties.get(0));
        }

        @Test
        public void shouldNotLockWhenUsingSharedKeys() throws InterruptedException {
            final SampleConfigObject config = new SampleConfigObject(configProperties);
            final TestConfigUpdater updater1 = new TestConfigUpdater(updateLock, updateKey1, "prop1", 10);
            final TestConfigUpdater updater2 = new TestConfigUpdater(updateLock, updateKey1, "prop2", 0);
            Thread t1 = new Thread() {
                @Override
                public void run() {
                    updater1.configurationUpdated(config);
                }
            };
            Thread t2 = new Thread() {
                @Override
                public void run() {
                    updater2.configurationUpdated(config);
                }
            };

            t1.start();
            t2.start();

            //Thread.sleep(20);

            t1.join();
            t2.join();

            assertEquals("prop2", configProperties.get(0));
        }
    }

    private static class TestConfigUpdater extends LockedConfigurationUpdater<SampleConfigObject> {
        String property;
        int sleepTime;

        public TestConfigUpdater(KeyedStackLock updateLock, Object updateKey, String property, int sleepTime) {
            super(updateLock, updateKey);
            this.property = property;
            this.sleepTime = sleepTime;
        }

        @Override
        protected void onConfigurationUpdated(SampleConfigObject configurationObject) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Ugh, I can't sleep!", e);
            }

            configurationObject.update(property);
        }
    }

    private static class SampleConfigObject {
        private List<String> configProperties;

        public SampleConfigObject(List<String> configProperties) {
            this.configProperties = configProperties;
        }

        public void update(String property) {
            configProperties.add(property);
        }
    }
}
