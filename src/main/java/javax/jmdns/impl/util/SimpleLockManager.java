/*
 * Copyright Object Matrix 2017
 */

package javax.jmdns.impl.util;

import java.io.Closeable;
import java.io.Serial;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Basic lock manager which uses internal {@link Map} to maintain a list of current locks.
 */
public class SimpleLockManager {

    private final ConcurrentHashMap<String, Locked> _locks = new ConcurrentHashMap<>();

    public Locked lock(String lockKey) {
        try {
            return tryLock(lockKey, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (LockFailedException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public Locked tryLock(String lockKey, long time, TimeUnit timeunit) throws InterruptedException, LockFailedException {
        Locked newLock = new LockedImpl(lockKey);
        long timeoutNanos = timeunit.toNanos(time);
        long startAt = System.nanoTime();
        while (true) {
            Locked previous = _locks.putIfAbsent(lockKey, newLock);
            if (previous == null)
                return newLock;

            if ((System.nanoTime() - startAt) >= timeoutNanos)
                throw new LockFailedException();

            Thread.sleep(10);
        }
    }

    private class LockedImpl extends Locked {

        private final String id;

        private LockedImpl(String id) {
            this.id = id;
        }

        @SuppressWarnings("resource")
        @Override
        public void close() {
            _locks.remove(id);
        }
    }
    
    public static abstract class Locked implements Closeable {
        @Override
        public abstract void close();
    }
    
    public static class LockFailedException extends Exception {

        @Serial
        private static final long serialVersionUID = 1L;

    }
}