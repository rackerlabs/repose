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
package org.openrepose.commons.utils.thread;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 *
 */
public class KeyedStackLockTest {

    private static final Object KEY_A = new Object();
    private static final Object KEY_B = new Object();
    private KeyedStackLock lock;

    @Before
    public void standUp() {
        lock = new KeyedStackLock();
    }

    @Test
    public void shouldUnlockIfSameKey() throws Exception {
        lock.tryLock(KEY_A);

        assertTrue("before", lock.isLocked());

        lock.unlock(KEY_A);

        assertFalse("after", lock.isLocked());
    }

    //TODO: finish test
    @Test
    public void shouldUnlockOneOfManyIfSameKey() throws Exception {
        assertTrue("KEY_A", lock.tryLock(KEY_A));

        //assertTrue("KEY_B", lock.tryLock(KEY_B));

        //lock.unlock(KEY_A);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfTryingToUnlockUsingWrongKey() throws Exception {
        assertTrue("is now locked", lock.tryLock(KEY_A));

        lock.unlock(KEY_B);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailIfNotLocked() throws Exception {

        lock.unlock(KEY_A);
    }

    @Test(expected = IllegalMonitorStateException.class)
    public void whenUnlockingShouldQueueMultipleLockRequestsUsingDifferentKeys() throws Exception {
        final TurnKeyLockingThread otherLock = new TurnKeyLockingThread(lock, KEY_A);

        assertThreads("Should lock", otherLock);

        lock.unlock(KEY_A);
    }

    @Test
    public void shouldProcessQueuedLockRequestsUsingDifferentKeys() throws Exception {
        final TurnKeyLockingThread threadA = new TurnKeyLockingThread(lock, KEY_A);
        final TurnKeyLockingBlockingThread threadB = new TurnKeyLockingBlockingThread(lock, KEY_B);

        assertThreads("Should lock with thread", threadA);
        assertThreads("Should lock with different key", threadA, threadB);
        assertThreads("Should lock with different key (2)", threadB);
    }

    @Test
    public void whenLockingShouldQueueMultipleLockRequestsUsingDifferentKeys() throws Exception {
        assertTrue(lock.tryLock(KEY_A));
        assertFalse(lock.tryLock(KEY_B));
    }

    @Test
    public void shouldPassIfSameThreadAttemptsToLockWithSameKey() throws Exception {
        assertTrue(lock.tryLock(KEY_A));
        assertTrue(lock.tryLock(KEY_A));
    }

    @Test
    public void shouldFailWithDifferentKey() throws Exception {
        assertFalse("is not locked yet", lock.isLocked());

        assertThreads("Should lock when unlocked", new TurnKeyLockingThread(lock, KEY_A));

        assertTrue("should now be locked", lock.isLocked());

        assertFalse("Parent thread should fail to lock successfully while using a different key",
                lock.tryLock(KEY_B));
    }

    @Test
    public void shouldPassWithSameKey() throws Exception {
        assertThreads("Should pass multiple threads using the same key",
                new TurnKeyLockingThread(lock, KEY_A), new TurnKeyLockingThread(lock, KEY_A));
    }

    private static void assertThreads(String msg, KeyedStackLockTestThread... threads) throws InterruptedException {
        for (KeyedStackLockTestThread t : threads) {
            if (!t.started()) {
                t.exec();
            }
        }

        int iterations = 0;

        while (!threadsFinished(threads) && ++iterations < 50) {
            Thread.sleep(10);
        }

        for (KeyedStackLockTestThread t : threads) {
            assertTrue(msg, t.passed());
        }
    }

    private static boolean threadsFinished(KeyedStackLockTestThread... threads) {
        for (KeyedStackLockTestThread t : threads) {
            if (!t.finished()) {
                return false;
            }
        }

        return true;
    }
}
