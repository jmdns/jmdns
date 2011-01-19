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
         * Checks if the entry is expired.
         *
         * @param now
         *            current time in milliseconds
         * @return <code>true</code> if the entry is expired, <code>false</code> otherwise.
         */
        boolean isExpired(long now) {
            return this._expiry <= now;
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
            long now = System.currentTimeMillis();
            return "Semaphore: " + this._semaphore + (this.isExpired(now) ? " expired." : " expires in: " + (this._expiry - now) + "ms.");
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
        if (timeout > 0) {
            _timer.schedule(new ClearExpiredEntriesTask(this), timeout);
        }
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
            if (semaphoreEntry.isExpired(now)) {
                semaphoreEntry.getSemaphore().release();
                entries.remove(semaphoreEntry);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder aLog = new StringBuilder(1000);
        aLog.append("Semaphore: ");
        aLog.append(this._name);
        if (_semaphores.size() == 0) {
            aLog.append(" no entries.");
        } else {
            aLog.append(" entries:\n");
            for (Thread thread : _semaphores.keySet()) {
                aLog.append("\tThread: ");
                aLog.append(thread.getName());
                aLog.append(' ');
                aLog.append(_semaphores.get(thread));
                aLog.append('\n');
            }
        }
        return aLog.toString();
    }

}
