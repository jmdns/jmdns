/*
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
 */
package javax.jmdns.impl.util;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Basic lock manager which uses internal {@link Map} to maintain a list of current {@link Locked} objects representing a exclusively locked resource.
 */
public class SimpleLockManager {

    private final ConcurrentHashMap<String, Locked> _locks = new ConcurrentHashMap<>();

    /**
     * Acquires a {@link Locked} object for a resource with a given key.
     * <p>
     * This method blocks indefinitely
     * 
     * @param lockKey
     *            key representing a locked object
     * @return instance of {@link Locked}
     */
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

    
    /**
     * Attempts to acquire a {@link Locked} object for a resource with a given key within specified time.
     * <p>
     * 
     * @param lockKey
     *            key representing a locked object
     * @throws LockFailedException
     *             if failed to acquire a lock within specified time
     * @return instance of {@link Locked}
     */
    @SuppressWarnings("resource")
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
    
    /**
     * Represents a locked resource.
     * <p>
     * Client should obtain Locked in try-with-resource block to automatically call {@link Locked#close()
     */
    public abstract static class Locked implements Closeable {
        @Override
        public abstract void close();
    }
    
    /**
     * Indicates {@link Locked} could not be acquired e.g. due to timeout.
     */
    public static class LockFailedException extends Exception {

        private static final long serialVersionUID = 1L;

    }
}
