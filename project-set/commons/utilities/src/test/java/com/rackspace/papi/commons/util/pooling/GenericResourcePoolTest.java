package com.rackspace.papi.commons.util.pooling;

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
            resourcePool = new GenericBlockingResourcePool<Target>(new ConstructionStrategy<Target>() {
                
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
            resourcePool = new GenericBlockingResourcePool<Target>(new ConstructionStrategy<Target>() {
                
                @Override
                public Target construct() {
                    return new Target();
                }
            }, 2, 6);
        }
        
        @Test
        public void shouldGenerateNewResources() {
            resourcePool = new GenericBlockingResourcePool<Target>(new ConstructionStrategy<Target>() {
                
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
