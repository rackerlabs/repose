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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 *
 */
@RunWith(Enclosed.class)
public class GenericResourcePoolTest {

    static class Target {
    }

    public static class WhenResizingPools {

        private Pool<Target> resourcePool;

        @Before
        public void standUp() {
            resourcePool = new GenericBlockingResourcePool<>(new ConstructionStrategy<Target>() {

                @Override
                public Target construct() {
                    return new Target();
                }
            });
        }

        @Test
        public void shouldPrimePoolWithMinimumSize() {
            resourcePool.setMinimumPoolSize(2);
            assertTrue(resourcePool.size() == 2);
        }

        @Test
        public void shouldReducePoolWithMaximumSize() {
            resourcePool.setMinimumPoolSize(4);
            resourcePool.setMinimumPoolSize(1);
            resourcePool.setMaximumPoolSize(3);

            assertTrue(resourcePool.size() == 3);
        }
    }

    public static class WhenAccessingPooledResources {

        private volatile boolean go;
        private volatile int internalThreadCount;
        private Pool<Target> resourcePool;

        @Before
        public void standUp() {
            resourcePool = new GenericBlockingResourcePool<>(new ConstructionStrategy<Target>() {

                @Override
                public Target construct() {
                    return new Target();
                }
            }, 2, 6);
        }

        @Test
        public void shouldGenerateNewResources() {
            resourcePool = new GenericBlockingResourcePool<>(new ConstructionStrategy<Target>() {

                @Override
                public Target construct() {
                    return new Target();
                }
            }, 0, 5);

            resourcePool.use(new SimpleResourceContext<Target>() {

                @Override
                public void perform(Target resource) {
                    assertNotNull(resource);
                }
            });

            assertTrue(1 == resourcePool.size());
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
            go = false;

            internalThreadCount = 0;

            final SimpleResourceContext<Target> context = new SimpleResourceContext<Target>() {

                @Override
                public void perform(Target resource) throws ResourceContextException {
                    try {
                        while (!go) {
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException ie) {
                    }
                }
            };

            for (int c = 0; c < 8; c++) {
                final Thread t = new Thread("Thread: " + c) {

                    @Override
                    public void run() {
                        internalThreadCount++;

                        try {
                            resourcePool.use(context);
                        } finally {
                            internalThreadCount--;
                        }
                    }
                };

                t.start();
            }

            while (internalThreadCount < 7) {
                Thread.sleep(10);
            }

            assertTrue("Resource pool should have a max size of 6. Was: " + resourcePool.size(), resourcePool.size() == 6);

            go = true;

            while (internalThreadCount > 0) {
                Thread.sleep(10);
            }
        }
    }
}
