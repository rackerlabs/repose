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
import org.junit.Test;
import org.openrepose.commons.utils.thread.KeyedStackLock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 7/6/11
 * Time: 1:32 PM
 */
public class LockedConfigurationUpdaterTest {
    private KeyedStackLock updateLock;
    private Object updateKey1, updateKey2;
    private List<String> configProperties;

    @Before
    public void setup() {
        updateLock = new KeyedStackLock();
        updateKey1 = new Object();
        updateKey2 = new Object();
        configProperties = new ArrayList<>();
    }

    @Test
    public void shouldLockWhenUsingUniqueKeys() throws InterruptedException {
        final SampleConfigObject config = new SampleConfigObject(configProperties);
        final TestConfigUpdater updater2 = new TestConfigUpdater(updateLock, updateKey2, "prop2", 0, null, false);
        Thread t2 = new Thread(() -> updater2.configurationUpdated(config));

        final TestConfigUpdater updater1 = new TestConfigUpdater(updateLock, updateKey1, "prop1", 10, t2, false);
        Thread t1 = new Thread(() -> updater1.configurationUpdated(config));

        t1.start();

        t1.join();
        t2.join();

        assertThat(configProperties, contains("prop1", "prop2"));
    }

    @Test
    public void shouldNotLockWhenUsingSharedKeys() throws InterruptedException {
        final SampleConfigObject config = new SampleConfigObject(configProperties);
        final TestConfigUpdater updater2 = new TestConfigUpdater(updateLock, updateKey1, "prop2", 0, null, true);
        Thread t2 = new Thread(() -> updater2.configurationUpdated(config));

        final TestConfigUpdater updater1 = new TestConfigUpdater(updateLock, updateKey1, "prop1", 0, t2, true);
        Thread t1 = new Thread(() -> updater1.configurationUpdated(config));

        t1.start();
        t1.join();

        assertThat(configProperties, contains("prop2", "prop1"));
    }

    private static class TestConfigUpdater extends LockedConfigurationUpdater<SampleConfigObject> {
        String property;
        int sleepTime;
        Thread nextThread;
        boolean joinThread;

        TestConfigUpdater(KeyedStackLock updateLock, Object updateKey, String property, int sleepTime, Thread nextThread, boolean joinThread) {
            super(updateLock, updateKey);
            this.property = property;
            this.sleepTime = sleepTime;
            this.nextThread = nextThread;
            this.joinThread = joinThread;
        }

        @Override
        protected void onConfigurationUpdated(SampleConfigObject configurationObject) {
            Optional<Thread> thread = Optional.ofNullable(nextThread);

            // At this point, we should have obtained the lock, so we can start the thread chain
            thread.ifPresent(Thread::start);

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Ugh, I can't sleep!", e);
            }

            // At this point, we may want the started thread to finish it's processing before
            // processing for this thread.
            thread.filter(__ -> joinThread).ifPresent(TestConfigUpdater::doJoinThread);

            configurationObject.update(property);
        }

        private static void doJoinThread(Thread t) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Ugh, I can't join the thread!", e);
            }
        }
    }

    private static class SampleConfigObject {
        private List<String> configProperties;

        SampleConfigObject(List<String> configProperties) {
            this.configProperties = configProperties;
        }

        void update(String property) {
            configProperties.add(property);
        }
    }
}
