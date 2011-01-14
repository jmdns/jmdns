/**
 *
 */
package javax.jmdns.impl;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class define a semaphore. On this multiple threads can wait the arrival of one event. Thread wait for a maximum defined by the timeout.
 * <p>
 * Implementation note: this class is based on {@link java.util.concurrent.Semaphore} so that they can be released by the timeout timer.
 * </p>
 *
 * @author Pierre Frisch
 */
public class DNSStatefulObjectSemaphore {
    private static Logger                      logger = Logger.getLogger(DNSStatefulObjectSemaphore.class.getName());

    private static final Timer                 _timer = new Timer("State Semaphore Timeout", true);

    private final String                       _name;

    private final ConcurrentMap<Thread, Entry> _semaphores;

    /**
     * This is the entry for each thread waiting on the event.
     */
    private static final class Entry {

        private final Semaphore _semaphore;

        private long            _expiry;

        public Entry(long timeout) {
            super();
            _semaphore = new Semaphore(1, true);
            _semaphore.drainPermits();
            _expiry = timeout;
        }

        /**
         * Returns the expiry date of this entry
         *
         * @return the expiry date of this entry
         */
        long getExpiryDate() {
            return this._expiry;
        }

        /**
         * Sets the expiry date of this entry
         *
         * @param value
         *            the expiry date of this entry
         */
        void setExpiryDate(long value) {
            this._expiry = value;
        }

        /**
         * Returns the semaphore for this entry
         *
         * @return the semaphore
         */
        Semaphore getSemaphore() {
            return this._semaphore;
        }

        @Override
        public String toString() {
            return "Semaphore: " + this._semaphore + " timeout: " + (this._expiry - System.currentTimeMillis());
        }
    }

    private static final class ClearExpiredEntriesTask extends TimerTask {

        private final DNSStatefulObjectSemaphore _semaphore;

        public ClearExpiredEntriesTask(DNSStatefulObjectSemaphore semaphore) {
            super();
            _semaphore = semaphore;
        }

        /*
         * (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            _semaphore.clearExpiredEntries();
        }

    }

    /**
     * @param name
     *            Semaphore name for debugging purposes.
     */
    public DNSStatefulObjectSemaphore(String name) {
        super();
        _name = name;
        _semaphores = new ConcurrentHashMap<Thread, Entry>(50);
    }

    /**
     * Blocks the current thread until the event arrives or the timeout expires.
     *
     * @param timeout
     *            wait period for the event
     */
    public void waitForEvent(long timeout) {
        long end = (timeout > 0 ? System.currentTimeMillis() + timeout : Long.MAX_VALUE);
        Thread thread = Thread.currentThread();
        Entry semaphoreEntry = _semaphores.get(thread);
        if (semaphoreEntry == null) {
            _semaphores.putIfAbsent(thread, new Entry(end));
        }
        semaphoreEntry = _semaphores.get(thread);
        try {
            semaphoreEntry.setExpiryDate(end);
            semaphoreEntry.getSemaphore().acquire();
        } catch (InterruptedException exception) {
            logger.log(Level.FINER, "Exception ", exception);
        }
        if (timeout > 0) {
            _timer.schedule(new ClearExpiredEntriesTask(this), timeout);
        }
    }

    /**
     * Signals the semaphore when the event arrives.
     */
    public void signalEvent() {
        Collection<Entry> entries = _semaphores.values();
        for (Entry semaphoreEntry : entries) {
            semaphoreEntry.getSemaphore().release();
            entries.remove(semaphoreEntry);
        }
    }

    /**
     * Clears expired entries to satisfy the timeout.
     */
    protected void clearExpiredEntries() {
        long now = System.currentTimeMillis();
        Collection<Entry> entries = _semaphores.values();
        for (Entry semaphoreEntry : entries) {
            if (semaphoreEntry.getExpiryDate() < now) {
                semaphoreEntry.getSemaphore().release();
                entries.remove(semaphoreEntry);
            }
        }
    }

    @Override
    public String toString() {
        return "Semaphore: " + this._name;
    }

}
