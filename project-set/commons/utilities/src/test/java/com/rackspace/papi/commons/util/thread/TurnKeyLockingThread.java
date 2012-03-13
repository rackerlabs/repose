package com.rackspace.papi.commons.util.thread;

/**
 *
 * 
 */
public class TurnKeyLockingThread extends Thread implements KeyedStackLockTestThread {

    protected final KeyedStackLock lockReference;
    protected final Object key;
    /// TODO: Review.  Should any of these be marked as volatile?  Or should their updates and reads be synchronized?
    protected boolean finished, passed, run, shouldStop, lock;

    public TurnKeyLockingThread(KeyedStackLock lockReference, Object key) {
        this.lockReference = lockReference;
        this.key = key;

        finished = false;
        passed = false;
        run = false;
        shouldStop = false;
        lock = true;

        super.start();
    }

    protected void toggleLockState() {
        if (lock) {
            passed = lockReference.tryLock(key);
        } else {
            lockReference.unlock(key);
        }

        lock = !lock;
    }

    @Override
    public synchronized void exec() {
        run = true;
        notify();
    }

    @Override
    public void kill() {
        shouldStop = true;
    }

    @Override
    public void run() {
        while (!shouldStop) {
            synchronized (this) {
                if (!run) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                        return;
                    }
                } else {
                    finished = false;
                    toggleLockState();
                    finished = true;

                    run = false;
                }
            }
        }
    }

    @Override
    public boolean started() {
        return run;
    }

    @Override
    public boolean finished() {
        return finished;
    }

    @Override
    public boolean passed() {
        return passed;
    }
}
