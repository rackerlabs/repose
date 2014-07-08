package com.rackspace.papi.service.threading.impl

import org.junit.Before
import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat

class ThreadingServgiceImplTest {
    ThreadingServiceImpl threadingService

    Runnable emptyRunnable = new Runnable() {
        @Override
        void run() {
            // do nothing
        }
    }

    @Before
    void setup() {
        threadingService = new ThreadingServiceImpl()
    }

    @Test
    void newThreadShouldReturnAThreadInTheNewState() {
        Thread thread = threadingService.newThread(emptyRunnable, "Test")

        assertThat(thread.getState(), equalTo(Thread.State.NEW))
    }

    @Test
    void newThreadShouldKeepAWeakReferenceToEachThreadItCreates() {
        Thread thread = threadingService.newThread(emptyRunnable, "Test2")

        assertThat(threadingService.liveThreadReferences.iterator().next().get(), equalTo(thread))
    }
}
