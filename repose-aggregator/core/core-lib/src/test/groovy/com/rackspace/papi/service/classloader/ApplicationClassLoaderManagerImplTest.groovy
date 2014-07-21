package com.rackspace.papi.service.classloader

import com.oracle.javaee6.FilterType
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoader
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext
import com.rackspace.papi.commons.util.classloader.ear.EarDescriptor
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.Matchers.contains
import static org.junit.Assert.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ApplicationClassLoaderManagerImplTest {
    ApplicationClassLoaderManagerImpl applicationClassLoaderManager

    @Before
    void setup() {
        applicationClassLoaderManager = new ApplicationClassLoaderManagerImpl(null)
    }

    @Test
    void testRemoveApplication() {
        EarClassLoaderContext mockEarClassLoaderContext = mock(EarClassLoaderContext)

        applicationClassLoaderManager.classLoaderMap.put("test-context", mockEarClassLoaderContext)

        applicationClassLoaderManager.removeApplication("test-context")

        assertNull(applicationClassLoaderManager.classLoaderMap.get("test-context"))
    }

    @Test
    void testPutApplication() {
        EarClassLoaderContext mockEarClassLoaderContext = mock(EarClassLoaderContext)

        applicationClassLoaderManager.putApplication("test-context", mockEarClassLoaderContext)

        assertThat(applicationClassLoaderManager.classLoaderMap.get("test-context"), equalTo(mockEarClassLoaderContext))
    }

    @Test
    void testHasFilter() {
        HashMap<String, FilterType> filterMap = new HashMap<>()
        filterMap.put("test-filter", null)

        EarDescriptor mockEarDescriptor = mock(EarDescriptor)
        EarClassLoaderContext mockEarClassLoaderContext = mock(EarClassLoaderContext)

        when(mockEarClassLoaderContext.getEarDescriptor()).thenReturn(mockEarDescriptor)
        when(mockEarDescriptor.getRegisteredFilters()).thenReturn(filterMap)

        applicationClassLoaderManager.classLoaderMap.put("test-context", mockEarClassLoaderContext)

        assertTrue(applicationClassLoaderManager.hasFilter("test-filter"))
    }

    @Test
    void testGetApplication() {
        EarClassLoader mockEarClassLoader = mock(EarClassLoader)
        EarClassLoaderContext mockEarClassLoaderContext = mock(EarClassLoaderContext)

        when(mockEarClassLoaderContext.getClassLoader()).thenReturn(mockEarClassLoader)

        applicationClassLoaderManager.classLoaderMap.put("test-context", mockEarClassLoaderContext)

        EarClassLoader returnedContext = applicationClassLoaderManager.getApplication("test-context")

        assertThat(returnedContext, equalTo(mockEarClassLoader))
    }

    @Test
    void testGetLoadedApplications() {
        EarClassLoaderContext mockEarClassLoaderContext = mock(EarClassLoaderContext)

        applicationClassLoaderManager.classLoaderMap.put("test-context", mockEarClassLoaderContext)

        assertThat(applicationClassLoaderManager.getLoadedApplications(), contains(mockEarClassLoaderContext))
    }
}
