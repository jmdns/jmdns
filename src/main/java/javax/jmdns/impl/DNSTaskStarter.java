/**
 *
 */
package javax.jmdns.impl;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.jmdns.JmmDNS;
import javax.jmdns.impl.tasks.RecordReaper;
import javax.jmdns.impl.tasks.Responder;
import javax.jmdns.impl.tasks.resolver.ServiceInfoResolver;
import javax.jmdns.impl.tasks.resolver.ServiceResolver;
import javax.jmdns.impl.tasks.resolver.TypeResolver;
import javax.jmdns.impl.tasks.state.Announcer;
import javax.jmdns.impl.tasks.state.Canceler;
import javax.jmdns.impl.tasks.state.Prober;
import javax.jmdns.impl.tasks.state.Renewer;

/**
 * This class is used by JmDNS to start the various task required to run the DNS discovery. This interface is only there in order to support MANET modifications.
 * <p>
 * <b>Note: </b> This is not considered as part of the general public API of JmDNS.
 * </p>
 */
public interface DNSTaskStarter {

    /**
     * DNSTaskStarter.Factory enable the creation of new instance of DNSTaskStarter.
     */
    public static final class Factory {

        private static volatile Factory                        _instance;
        private final ConcurrentMap<JmDNSImpl, DNSTaskStarter> _instances;

        /**
         * This interface defines a delegate to the DNSTaskStarter class to enable subclassing.
         */
        public static interface ClassDelegate {

            /**
             * Allows the delegate the opportunity to construct and return a different DNSTaskStarter.
             *
             * @param jmDNSImpl
             *            jmDNS instance
             * @return Should return a new DNSTaskStarter Object.
             * @see #classDelegate()
             * @see #setClassDelegate(ClassDelegate anObject)
             */
            public DNSTaskStarter newDNSTaskStarter(JmDNSImpl jmDNSImpl);
        }

        private static final AtomicReference<Factory.ClassDelegate> _databaseClassDelegate = new AtomicReference<Factory.ClassDelegate>();

        private Factory() {
            super();
            _instances = new ConcurrentHashMap<JmDNSImpl, DNSTaskStarter>(20);
        }

        /**
         * Assigns <code>delegate</code> as DNSTaskStarter's class delegate. The class delegate is optional.
         *
         * @param delegate
         *            The object to set as DNSTaskStarter's class delegate.
         * @see #classDelegate()
         * @see JmmDNS.Factory.ClassDelegate
         */
        public static void setClassDelegate(Factory.ClassDelegate delegate) {
            _databaseClassDelegate.set(delegate);
        }

        /**
         * Returns DNSTaskStarter's class delegate.
         *
         * @return DNSTaskStarter's class delegate.
         * @see #setClassDelegate(ClassDelegate anObject)
         * @see JmmDNS.Factory.ClassDelegate
         */
        public static Factory.ClassDelegate classDelegate() {
            return _databaseClassDelegate.get();
        }

        /**
         * Returns a new instance of DNSTaskStarter using the class delegate if it exists.
         *
         * @param jmDNSImpl
         *            jmDNS instance
         * @return new instance of DNSTaskStarter
         */
        protected static DNSTaskStarter newDNSTaskStarter(JmDNSImpl jmDNSImpl) {
            DNSTaskStarter instance = null;
            Factory.ClassDelegate delegate = _databaseClassDelegate.get();
            if (delegate != null) {
                instance = delegate.newDNSTaskStarter(jmDNSImpl);
            }
            return (instance != null ? instance : new DNSTaskStarterImpl(jmDNSImpl));
        }

        /**
         * Return the instance of the DNSTaskStarter Factory.
         *
         * @return DNSTaskStarter Factory
         */
        public static Factory getInstance() {
            if (_instance == null) {
                synchronized (DNSTaskStarter.Factory.class) {
                    if (_instance == null) {
                        _instance = new Factory();
                    }
                }
            }
            return _instance;
        }

        /**
         * Return the instance of the DNSTaskStarter for the JmDNS.
         *
         * @param jmDNSImpl
         *            jmDNS instance
         * @return the DNSTaskStarter
         */
        public DNSTaskStarter getStarter(JmDNSImpl jmDNSImpl) {
            DNSTaskStarter starter = _instances.get(jmDNSImpl);
            if (starter == null) {
                _instances.putIfAbsent(jmDNSImpl, newDNSTaskStarter(jmDNSImpl));
                starter = _instances.get(jmDNSImpl);
            }
            return starter;
        }

    }

    public static final class DNSTaskStarterImpl implements DNSTaskStarter {

        private final JmDNSImpl   _jmDNSImpl;

        /**
         * The timer is used to dispatch all outgoing messages of JmDNS. It is also used to dispatch maintenance tasks for the DNS cache.
         */
        private final Timer _timer;

        /**
         * The timer is used to dispatch maintenance tasks for the DNS cache.
         */
        private final Timer _stateTimer;

        public DNSTaskStarterImpl(JmDNSImpl jmDNSImpl) {
            super();
            _jmDNSImpl = jmDNSImpl;
            _timer = new Timer("JmDNS(" + _jmDNSImpl.getName() + ").Timer", true);
            _stateTimer = new Timer("JmDNS(" + _jmDNSImpl.getName() + ").State.Timer", false);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#purgeTimer()
         */
        @Override
        public void purgeTimer() {
            _timer.purge();
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#purgeStateTimer()
         */
        @Override
        public void purgeStateTimer() {
            _stateTimer.purge();
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#cancelTimer()
         */
        @Override
        public void cancelTimer() {
            _timer.cancel();
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#cancelStateTimer()
         */
        @Override
        public void cancelStateTimer() {
            _stateTimer.cancel();
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#startProber()
         */
        @Override
        public void startProber() {
            new Prober(_jmDNSImpl).start(_stateTimer);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#startAnnouncer()
         */
        @Override
        public void startAnnouncer() {
            new Announcer(_jmDNSImpl).start(_stateTimer);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#startRenewer()
         */
        @Override
        public void startRenewer() {
            new Renewer(_jmDNSImpl).start(_stateTimer);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#startCanceler()
         */
        @Override
        public void startCanceler() {
            new Canceler(_jmDNSImpl).start(_stateTimer);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#startReaper()
         */
        @Override
        public void startReaper() {
            new RecordReaper(_jmDNSImpl).start(_timer);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#startServiceInfoResolver(javax.jmdns.impl.ServiceInfoImpl)
         */
        @Override
        public void startServiceInfoResolver(ServiceInfoImpl info) {
            new ServiceInfoResolver(_jmDNSImpl, info).start(_timer);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#startTypeResolver()
         */
        @Override
        public void startTypeResolver() {
            new TypeResolver(_jmDNSImpl).start(_timer);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#startServiceResolver(java.lang.String)
         */
        @Override
        public void startServiceResolver(String type) {
            new ServiceResolver(_jmDNSImpl, type).start(_timer);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSTaskStarter#startResponder(javax.jmdns.impl.DNSIncoming, int)
         */
        @Override
        public void startResponder(DNSIncoming in, int port) {
            new Responder(_jmDNSImpl, in, port).start(_timer);
        }
    }

    /**
     * Purge the general task timer
     */
    public void purgeTimer();

    /**
     * Purge the state task timer
     */
    public void purgeStateTimer();

    /**
     * Cancel the generals task timer
     */
    public void cancelTimer();

    /**
     * Cancel the state task timer
     */
    public void cancelStateTimer();

    /**
     * Start a new prober task
     */
    public void startProber();

    /**
     * Start a new announcer task
     */
    public void startAnnouncer();

    /**
     * Start a new renewer task
     */
    public void startRenewer();

    /**
     * Start a new canceler task
     */
    public void startCanceler();

    /**
     * Start a new reaper task. There is only supposed to be one reaper running at a time.
     */
    public void startReaper();

    /**
     * Start a new service info resolver task
     *
     * @param info
     *            service info to resolve
     */
    public void startServiceInfoResolver(ServiceInfoImpl info);

    /**
     * Start a new service type resolver task
     */
    public void startTypeResolver();

    /**
     * Start a new service resolver task
     *
     * @param type
     *            service type to resolve
     */
    public void startServiceResolver(String type);

    /**
     * Start a new responder task
     *
     * @param in
     *            incoming message
     * @param port
     *            incoming port
     */
    public void startResponder(DNSIncoming in, int port);

}
