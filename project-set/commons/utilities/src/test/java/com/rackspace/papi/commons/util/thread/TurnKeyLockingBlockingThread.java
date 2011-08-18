package com.rackspace.papi.commons.util.thread;

/**
 *
 * 
 */
public class TurnKeyLockingBlockingThread extends TurnKeyLockingThread {

    public TurnKeyLockingBlockingThread(KeyedStackLock lockReference, Object key) {
        super(lockReference, key);
    }

    @Override
    protected void toggleLockState() {
        if (lock) {
            lockReference.lock(key);
            passed = true;
        } else {
            lockReference.unlock(key);
        }

        lock = !lock;
    }
}
