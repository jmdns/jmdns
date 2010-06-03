/**
 *
 */
package javax.jmdns.impl;

import javax.jmdns.impl.constants.DNSState;
import javax.jmdns.impl.tasks.DNSTask;

/**
 *Sets of methods to manage the state machine.<br/>
 *<b>Implementation note:</b> This interface is accessed from multiple threads. The implementation must be thread safe.
 */
public interface DNSStatefulObject
{

    public static class DefaultImplementation implements DNSStatefulObject
    {

        private volatile JmDNSImpl _dns;

        protected volatile DNSTask _task;

        protected volatile DNSState _state;

        public DefaultImplementation()
        {
            super();
            _dns = null;
            _task = null;
            _state = DNSState.PROBING_1;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefullObject#getDns()
         */
        public JmDNSImpl getDns()
        {
            return this._dns;
        }

        protected void setDns(JmDNSImpl dns)
        {
            this._dns = dns;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefulObject#associateWithTask(javax.jmdns.impl.tasks.DNSTask, javax.jmdns.impl.constants.DNSState)
         */
        @Override
        public void associateWithTask(DNSTask task, DNSState state)
        {
            if (this._task == null && this._state == state)
            {
                synchronized (this)
                {
                    if (this._task == null && this._state == state)
                    {
                        this.setTask(task);
                    }
                }
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefulObject#removeAssociationWithTask(javax.jmdns.impl.tasks.DNSTask)
         */
        @Override
        public void removeAssociationWithTask(DNSTask task)
        {
            if (this._task == task)
            {
                synchronized (this)
                {
                    if (this._task == task)
                    {
                        this.setTask(null);
                    }
                }
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefullObject#isHandledByTask(javax.jmdns.impl.tasks.DNSTask, javax.jmdns.impl.constants.DNSState)
         */
        @Override
        public boolean isAssociatedWithTask(DNSTask task, DNSState state)
        {
            synchronized (this)
            {
                return this._task == task && this._state == state;
            }
        }

        protected void setTask(DNSTask task)
        {
            this._task = task;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefullObject#advanceState()
         */
        @Override
        public boolean advanceState()
        {
            boolean result = true;
            synchronized (this)
            {
                this._state = this._state.advance();
                this.notifyAll();
            }
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefullObject#revertState()
         */
        @Override
        public boolean revertState()
        {
            boolean result = true;
            synchronized (this)
            {
                this._state = this._state.revert();
                this.setTask(null);
                this.notifyAll();
            }
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefullObject#cancel()
         */
        @Override
        public boolean cancelState()
        {
            boolean result = false;
            if (!this.isCanceling())
            {
                synchronized (this)
                {
                    if (!this.isCanceling())
                    {
                        this._state = DNSState.CANCELING_1;
                        this.setTask(null);
                        this.notifyAll();
                        result = true;
                    }
                }
            }
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefulObject#recover()
         */
        @Override
        public boolean recoverState()
        {
            boolean result = false;
            synchronized (this)
            {
                this._state = DNSState.PROBING_1;
                this.setTask(null);
                this.notifyAll();
            }
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefullObject#isProbing()
         */
        @Override
        public boolean isProbing()
        {
            return this._state.isProbing();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefullObject#isAnnouncing()
         */
        @Override
        public boolean isAnnouncing()
        {
            return this._state.isAnnouncing();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefullObject#isAnnounced()
         */
        @Override
        public boolean isAnnounced()
        {
            return this._state.isAnnounced();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefulObject#isCanceling()
         */
        @Override
        public boolean isCanceling()
        {
            return this._state.isCanceling();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefullObject#isCanceled()
         */
        @Override
        public boolean isCanceled()
        {
            return this._state.isCanceled();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefulObject#waitForAnnounced(long)
         */
        @Override
        public boolean waitForAnnounced(long timeout)
        {
            try
            {
                synchronized (this)
                {
                    boolean finished = false;
                    long end = (timeout > 0 ? System.currentTimeMillis() + timeout : Long.MAX_VALUE);
                    while (!this.isAnnounced() && !finished)
                    {
                        this.wait(timeout);
                        finished = end <= System.currentTimeMillis();
                    }
                }
            }
            catch (final InterruptedException e)
            {
                // empty
            }
            return this.isAnnounced();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.impl.DNSStatefulObject#waitForCanceled(long)
         */
        @Override
        public boolean waitForCanceled(long timeout)
        {
            try
            {
                synchronized (this)
                {
                    boolean finished = false;
                    long end = (timeout > 0 ? System.currentTimeMillis() + timeout : Long.MAX_VALUE);
                    while (!this.isCanceled() && !finished)
                    {
                        this.wait(timeout);
                        finished = end <= System.currentTimeMillis();
                    }
                }
            }
            catch (final InterruptedException e)
            {
                // empty
            }
            return this.isCanceled();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return (_dns != null ? "DNS: " + _dns.getName() : "NO DNS") + " state: " + _state + " task: " + _task;
        }

    }

    /**
     * Returns the DNS associated with this object.
     *
     * @return DNS resolver
     */
    public JmDNSImpl getDns();

    /**
     * Sets the task associated with this Object.
     *
     * @param task
     *            associated task
     * @param state
     *            state of the task
     */
    public void associateWithTask(DNSTask task, DNSState state);

    /**
     * Remove the association of the task with this Object.
     *
     * @param task
     *            associated task
     */
    public void removeAssociationWithTask(DNSTask task);

    /**
     * Checks if this object is associated with the task and in the same state.
     *
     * @param task
     *            associated task
     * @param state
     *            state of the task
     * @return <code>true</code> is the task is associated with this object, <code>false</code> otherwise.
     */
    public boolean isAssociatedWithTask(DNSTask task, DNSState state);

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     *
     * @return <code>true</code if the state was changed by this thread, <code>false</code> otherwise.
     *
     * @see DNSState#advance()
     */
    public boolean advanceState();

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     *
     * @return <code>true</code if the state was changed by this thread, <code>false</code> otherwise.
     * @see DNSState#revert()
     */
    public boolean revertState();

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     *
     * @return <code>true</code if the state was changed by this thread, <code>false</code> otherwise.
     */
    public boolean cancelState();

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     *
     * @return <code>true</code if the state was changed by this thread, <code>false</code> otherwise.
     */
    public boolean recoverState();

    /**
     * Returns true, if this is a probing state.
     *
     * @return <code>true</code> if probing state, <code>false</code> otherwise
     */
    public boolean isProbing();

    /**
     * Returns true, if this is an announcing state.
     *
     * @return <code>true</code> if announcing state, <code>false</code> otherwise
     */
    public boolean isAnnouncing();

    /**
     * Returns true, if this is an announced state.
     *
     * @return <code>true</code> if announced state, <code>false</code> otherwise
     */
    public boolean isAnnounced();

    /**
     * Returns true, if this is a canceling state.
     *
     * @return <code>true</code> if canceling state, <code>false</code> otherwise
     */
    public boolean isCanceling();

    /**
     * Returns true, if this is a canceled state.
     *
     * @return <code>true</code> if canceled state, <code>false</code> otherwise
     */
    public boolean isCanceled();

    /**
     * Waits for the object to be announced.
     *
     * @param timeout
     *            the maximum time to wait in milliseconds.
     * @return <code>true</code> if the object is announced, <code>false</code> otherwise
     */
    public boolean waitForAnnounced(long timeout);

    /**
     * Waits for the object to be canceled.
     *
     * @param timeout
     *            the maximum time to wait in milliseconds.
     * @return <code>true</code> if the object is canceled, <code>false</code> otherwise
     */
    public boolean waitForCanceled(long timeout);

}
