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
package org.openrepose.commons.utils.pooling;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class GenericResourcePoolTest {

    static class Target {
    }

    public static class WhenResizingPools {

        private Pool<Target> resourcePool;

        @Before
        public void standUp() {
            resourcePool = new GenericBlockingResourcePool<>(Target::new);
        }

        @Test
        public void shouldPrimePoolWithMinimumSize() {
            resourcePool.setMinimumPoolSize(2);
            assertThat(resourcePool.size(), equalTo(2));
        }

        @Test
        public void shouldReducePoolWithMaximumSize() {
            resourcePool.setMinimumPoolSize(4);
            resourcePool.setMinimumPoolSize(1);
            resourcePool.setMaximumPoolSize(3);

            assertThat(resourcePool.size(), equalTo(3));
        }
    }

    public static class WhenAccessingPooledResources {

        private int minPoolSize = 2;
        private int maxPoolSize = 6;
        private Pool<Target> resourcePool;
        private volatile boolean run;
        private volatile int activeThreadCount;
        private volatile int activeResourceCount;

        @Before
        public void standUp() {
            resourcePool = new GenericBlockingResourcePool<>(Target::new, minPoolSize, maxPoolSize);
        }

        @Test
        public void shouldGenerateNewResources() {
            resourcePool = new GenericBlockingResourcePool<>(Target::new, 0, 5);

            resourcePool.use(new SimpleResourceContext<Target>() {
                @Override
                public void perform(Target resource) {
                    assertNotNull(resource);
                }
            });

            assertThat(resourcePool.size(), equalTo(1));
        }

        @Test
        public void shouldAllowReturnsFromContext() {
            assertTrue(resourcePool.use(new ResourceContext<Target, Boolean>() {
                @Override
                public Boolean perform(Target resource) {
                    return true;
                }
            }));
        }

        @Test
        public void shouldHandleThreadPressure() throws Exception {
            int numTestThreads = 8;

            run = true;
            activeThreadCount = 0;
            activeResourceCount = 0;

            final SimpleResourceContext<Target> context = new SimpleResourceContext<Target>() {
                @Override
                public void perform(Target resource) throws ResourceContextException {
                    activeResourceCount++;

                    try {
                        // Continue to "use" the pooled resource so that it is not released.
                        while (run) {
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        activeResourceCount--;
                    }
                }
            };

            for (int c = 0; c < numTestThreads; c++) {
                final Thread t = new Thread("Thread: " + c) {
                    @Override
                    public void run() {
                        activeThreadCount++;

                        try {
                            resourcePool.use(context);
                        } finally {
                            activeThreadCount--;
                        }
                    }
                };

                t.start();
            }

            // Verify that all threads have started running, and that as many as possible have been granted a
            // resource from the pool.
            // This wait it so that we are reasonably sure that the class under test has been accessed as expected so
            // that we can assert certain things.
            // Technically, all threads could be running but not all threads have requested a resource from the pool yet,
            // but we would still bypass this wait.
            // While that does mean that our test has the possibility to pass without cause (i.e., without being fully set up),
            // it also means that our test should never fail in a difficult to diagnose manner (i.e., be flaky).
            while (activeThreadCount < numTestThreads || activeResourceCount < maxPoolSize) {
                Thread.sleep(10);
            }

            assertEquals(numTestThreads, activeThreadCount);
            assertEquals(maxPoolSize, activeResourceCount);
            assertEquals(maxPoolSize, resourcePool.size());

            // Allow all of the running threads to complete.
            run = false;

            // Wait for all of the running threads to complete.
            while (activeThreadCount > 0) {
                Thread.sleep(10);
            }
        }
    }
}
