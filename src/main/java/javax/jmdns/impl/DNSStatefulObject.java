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
package javax.jmdns.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.impl.constants.DNSState;
import javax.jmdns.impl.tasks.DNSTask;

/**
 * Sets of methods to manage the state machine.<br/>
 * <b>Implementation note:</b> This interface is accessed from multiple threads. The implementation must be thread safe.
 *
 * @author Pierre Frisch
 */
public interface DNSStatefulObject {

    /**
     * This class define a semaphore. On this multiple threads can wait the arrival of one event. Thread wait for a maximum defined by the timeout.
     * <p>
     * Implementation note: this class is based on {@link java.util.concurrent.Semaphore} so that they can be released by the timeout timer.
     * </p>
     *
     * @author Pierre Frisch
     */
    final class DNSStatefulObjectSemaphore {
        private final Logger                          logger = LoggerFactory.getLogger(DNSStatefulObjectSemaphore.class);

        private final String                           _name;

        private final ConcurrentMap<Thread, Semaphore> _semaphores;

        /**
         * @param name
         *            Semaphore name for debugging purposes.
         */
        public DNSStatefulObjectSemaphore(String name) {
            super();
            _name = name;
            _semaphores = new ConcurrentHashMap<>(50);
        }

        /**
         * Blocks the current thread until the event arrives or the timeout expires.
         *
         * @param timeout
         *            wait period for the event
         */
        public void waitForEvent(long timeout) {
            Thread thread = Thread.currentThread();
            Semaphore semaphore = _semaphores.get(thread);
            if (semaphore == null) {
                semaphore = new Semaphore(1, true);
                semaphore.drainPermits();
                _semaphores.putIfAbsent(thread, semaphore);
            }
            semaphore = _semaphores.get(thread);
            try {
                semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                logger.debug("Exception ", exception);
            }
        }

        /**
         * Signals the semaphore when the event arrives.
         */
        public void signalEvent() {
            Collection<Semaphore> semaphores = _semaphores.values();
            for (Semaphore semaphore : semaphores) {
                semaphore.release();
                semaphores.remove(semaphore);
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(1000);
            sb.append("Semaphore: ");
            sb.append(this._name);
            if (_semaphores.isEmpty()) {
                sb.append(" no semaphores.");
            } else {
                sb.append(" semaphores:\n");
                for (final Map.Entry<Thread, Semaphore> entry : _semaphores.entrySet()) {
                    sb.append("\tThread: ");
                    sb.append(entry.getKey().getName());
                    sb.append(' ');
                    sb.append(entry.getValue());
                    sb.append('\n');
                }
            }
            return sb.toString();
        }

    }

    class DefaultImplementation extends ReentrantLock implements DNSStatefulObject {
        private final Logger                     logger           = LoggerFactory.getLogger(DefaultImplementation.class);

        private static final long                serialVersionUID = -3264781576883412227L;

        private volatile JmDNSImpl               _dns;

        protected volatile DNSTask               _task;

        protected volatile DNSState              _state;

        private final DNSStatefulObjectSemaphore _announcing;

        private final DNSStatefulObjectSemaphore _canceling;

        public DefaultImplementation() {
            super();
            _dns = null;
            _task = null;
            _state = DNSState.PROBING_1;
            _announcing = new DNSStatefulObjectSemaphore("Announce");
            _canceling = new DNSStatefulObjectSemaphore("Cancel");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JmDNSImpl getDns() {
            return this._dns;
        }

        protected void setDns(JmDNSImpl dns) {
            this._dns = dns;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void associateWithTask(DNSTask task, DNSState state) {
            if (this._task == null && this._state == state) {
                this.lock();
                try {
                    if (this._task == null && this._state == state) {
                        this.setTask(task);
                    }
                } finally {
                    this.unlock();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeAssociationWithTask(DNSTask task) {
            if (this._task == task) {
                this.lock();
                try {
                    if (this._task == task) {
                        this.setTask(null);
                    }
                } finally {
                    this.unlock();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAssociatedWithTask(DNSTask task, DNSState state) {
            this.lock();
            try {
                return this._task == task && this._state == state;
            } finally {
                this.unlock();
            }
        }

        protected void setTask(DNSTask task) {
            this._task = task;
        }

        /**
         * @param state
         *            the state to set
         */
        protected void setState(DNSState state) {
            this.lock();
            try {
                this._state = state;
                if (this.isAnnounced()) {
                    _announcing.signalEvent();
                }
                if (this.isCanceled()) {
                    _canceling.signalEvent();
                    // clear any waiting announcing
                    _announcing.signalEvent();
                }
            } finally {
                this.unlock();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean advanceState(DNSTask task) {
            boolean result = true;
            if (this._task == task) {
                this.lock();
                try {
                    if (this._task == task) {
                        this.setState(this._state.advance());
                    } else {
                        logger.warn("Trying to advance state when not the owner. owner: {} perpetrator: {}", this._task, task);
                    }
                } finally {
                    this.unlock();
                }
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean revertState() {
            boolean result = true;
            if (!this.willCancel()) {
                this.lock();
                try {
                    if (!this.willCancel()) {
                        this.setState(this._state.revert());
                        this.setTask(null);
                    }
                } finally {
                    this.unlock();
                }
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean cancelState() {
            boolean result = false;
            if (!this.willCancel()) {
                this.lock();
                try {
                    if (!this.willCancel()) {
                        this.setState(DNSState.CANCELING_1);
                        this.setTask(null);
                        result = true;
                    }
                } finally {
                    this.unlock();
                }
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean closeState() {
            boolean result = false;
            if (!this.willClose()) {
                this.lock();
                try {
                    if (!this.willClose()) {
                        this.setState(DNSState.CLOSING);
                        this.setTask(null);
                        result = true;
                    }
                } finally {
                    this.unlock();
                }
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean recoverState() {
            boolean result = false;
            this.lock();
            try {
                this.setState(DNSState.PROBING_1);
                this.setTask(null);
            } finally {
                this.unlock();
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isProbing() {
            return this._state.isProbing();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAnnouncing() {
            return this._state.isAnnouncing();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAnnounced() {
            return this._state.isAnnounced();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCanceling() {
            return this._state.isCanceling();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCanceled() {
            return this._state.isCanceled();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isClosing() {
            return this._state.isClosing();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isClosed() {
            return this._state.isClosed();
        }

        private boolean willCancel() {
            return this._state.isCanceled() || this._state.isCanceling();
        }

        private boolean willClose() {
            return this._state.isClosed() || this._state.isClosing();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean waitForAnnounced(long timeout) {
            if (!this.isAnnounced() && !this.willCancel()) {
                _announcing.waitForEvent(timeout + 10);
            }
            if (!this.isAnnounced()) {
                // When we run multihomed we need to check twice
                _announcing.waitForEvent(10);
                if (!this.isAnnounced()) {
                    if (this.willCancel() || this.willClose()) {
                        logger.debug("Wait for announced cancelled: {}", this);
                    } else {
                        logger.warn("Wait for announced timed out: {}", this);
                    }
                }
            }
            return this.isAnnounced();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean waitForCanceled(long timeout) {
            if (!this.isCanceled()) {
                _canceling.waitForEvent(timeout);
            }
            if (!this.isCanceled()) {
                // When we run multihomed we need to check twice
                _canceling.waitForEvent(10);
                if (!this.isCanceled() && !this.willClose()) {
                    logger.warn("Wait for canceled timed out: {}", this);
                }
            }
            return this.isCanceled();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            try {
                return (_dns != null ? "DNS: " + _dns.getName() + " [" + _dns.getInetAddress() + "]" : "NO DNS") + " state: " + _state + " task: " + _task;
            } catch (IOException exception) {
                return (_dns != null ? "DNS: " + _dns.getName() : "NO DNS") + " state: " + _state + " task: " + _task;
            }
        }

    }

    /**
     * Returns the DNS associated with this object.
     *
     * @return DNS resolver
     */
    JmDNSImpl getDns();

    /**
     * Sets the task associated with this Object.
     *
     * @param task
     *            associated task
     * @param state
     *            state of the task
     */
    void associateWithTask(DNSTask task, DNSState state);

    /**
     * Remove the association of the task with this Object.
     *
     * @param task
     *            associated task
     */
    void removeAssociationWithTask(DNSTask task);

    /**
     * Checks if this object is associated with the task and in the same state.
     *
     * @param task
     *            associated task
     * @param state
     *            state of the task
     * @return <code>true</code> is the task is associated with this object, <code>false</code> otherwise.
     */
    boolean isAssociatedWithTask(DNSTask task, DNSState state);

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     *
     * @param task
     *            associated task
     * @return <code>true</code> if the state was changed by this thread, <code>false</code> otherwise.
     * @see DNSState#advance()
     */
    boolean advanceState(DNSTask task);

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     *
     * @return <code>true</code> if the state was changed by this thread, <code>false</code> otherwise.
     * @see DNSState#revert()
     */
    boolean revertState();

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     *
     * @return <code>true</code> if the state was changed by this thread, <code>false</code> otherwise.
     */
    boolean cancelState();

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     *
     * @return <code>true</code> if the state was changed by this thread, <code>false</code> otherwise.
     */
    boolean closeState();

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     *
     * @return <code>true</code> if the state was changed by this thread, <code>false</code> otherwise.
     */
    boolean recoverState();

    /**
     * Returns true, if this is a probing state.
     *
     * @return <code>true</code> if probing state, <code>false</code> otherwise
     */
    boolean isProbing();

    /**
     * Returns true, if this is an announcing state.
     *
     * @return <code>true</code> if announcing state, <code>false</code> otherwise
     */
    boolean isAnnouncing();

    /**
     * Returns true, if this is an announced state.
     *
     * @return <code>true</code> if announced state, <code>false</code> otherwise
     */
    boolean isAnnounced();

    /**
     * Returns true, if this is a canceling state.
     *
     * @return <code>true</code> if canceling state, <code>false</code> otherwise
     */
    boolean isCanceling();

    /**
     * Returns true, if this is a canceled state.
     *
     * @return <code>true</code> if canceled state, <code>false</code> otherwise
     */
    boolean isCanceled();

    /**
     * Returns true, if this is a closing state.
     *
     * @return <code>true</code> if closing state, <code>false</code> otherwise
     */
    boolean isClosing();

    /**
     * Returns true, if this is a closed state.
     *
     * @return <code>true</code> if closed state, <code>false</code> otherwise
     */
    boolean isClosed();

    /**
     * Waits for the object to be announced.
     *
     * @param timeout
     *            the maximum time to wait in milliseconds.
     * @return <code>true</code> if the object is announced, <code>false</code> otherwise
     */
    boolean waitForAnnounced(long timeout);

    /**
     * Waits for the object to be canceled.
     *
     * @param timeout
     *            the maximum time to wait in milliseconds.
     * @return <code>true</code> if the object is canceled, <code>false</code> otherwise
     */
    boolean waitForCanceled(long timeout);

}
