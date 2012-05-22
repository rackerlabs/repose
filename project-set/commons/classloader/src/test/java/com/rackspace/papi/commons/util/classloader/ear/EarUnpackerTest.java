package com.rackspace.papi.commons.util.classloader.ear;

import com.rackspace.papi.commons.util.classloader.EarTestSupport;
import com.rackspace.papi.commons.util.classloader.jar.test.EmptyClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.security.Permission;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(Enclosed.class)
public class EarUnpackerTest {

    public static class WhenUnpackingEars extends EarTestSupport {

        private EarUnpacker unpacker;

        public WhenUnpackingEars() {
            super("edeploy");
        }

        @Override
        protected void onSetup() {
            unpacker = new EarUnpacker(getDeploymentDestination());
        }

        @Test
        public void shouldUnpackDirectoryStructure() throws Exception {
            final EarClassLoaderContext classLoaderContext = unpacker.read(
                    createEarArchiveEntryListener(unpacker.getDeploymentDirectory()),
                    createEarFile());

            final ClassLoader localClassLoader = classLoaderContext.getClassLoader();
            final Class<?> clazz = localClassLoader.loadClass(EmptyClass.class.getName());

            assertNotNull("Resolved, external class should not be null", clazz);
            assertEquals("External empty class copy should have a matching name", EmptyClass.class.getName(), clazz.getName());
            assertNotSame("Classloaders of externally sourced class files should be different from the default thread classloader", EmptyClass.class.getClassLoader(), clazz.getClassLoader());

            assertTrue("Should delete deployment directory", deleteDirectory(getDeploymentDestination()));
        }

        @Test @Ignore
        public void shouldNotAllowSystemExit() throws Exception {
            final EarClassLoaderContext classLoaderContext = unpacker.read(
                    createEarArchiveEntryListener(unpacker.getDeploymentDirectory()),
                    createEarFile());

            final ClassLoader localClassLoader = classLoaderContext.getClassLoader();
            final Class<?> clazz = localClassLoader.loadClass(EmptyClass.class.getName());

            final SecurityManager catchManager = new SecurityManager() {

                @Override
                public void checkPermission(Permission prmsn) {
                    if (prmsn.getName().contains("exitVM")) {
                        fail("Caught unhandled system exit!");
                    }
                }
            };
            
            final SecurityManager originalManager = System.getSecurityManager();

            try {
                System.setSecurityManager(catchManager);
                clazz.newInstance();
            } finally {
                System.setSecurityManager(originalManager);
                assertTrue("Should delete deployment directory", deleteDirectory(getDeploymentDestination()));
            }
        }
    }
}
