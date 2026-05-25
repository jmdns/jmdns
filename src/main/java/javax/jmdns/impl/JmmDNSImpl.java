/**
 *
 */
package javax.jmdns.impl;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.*;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.util.NamedThreadFactory;

/**
 * This class enable multihoming mDNS. It will open a mDNS per IP address of the machine.
 *
 * @author C&eacute;drik Lime, Pierre Frisch
 */
public class JmmDNSImpl implements JmmDNS, NetworkTopologyListener, ServiceInfoImpl.Delegate {

    private static Logger                                      logger = LoggerFactory.getLogger(JmmDNSImpl.class.getName());


    private final Set<NetworkTopologyListener>                 _networkListeners;

    /**
     * Every JmDNS created.
     */
    private final ConcurrentMap<InetAddress, JmDNS>            _knownMDNS;

    /**
     * This enable the service info text update.
     */
    private final ConcurrentMap<String, ServiceInfo>           _services;

    /**
     * List of registered services
     */
    private final Set<String>                                  _serviceTypes;

    /**
     * Holds instances of ServiceListener's. Keys are Strings holding a fully qualified service type. Values are LinkedList's of ServiceListener's.
     */
    private final ConcurrentMap<String, List<ServiceListener>> _serviceListeners;

    /**
     * Holds instances of ServiceTypeListener's.
     */
    private final Set<ServiceTypeListener>                     _typeListeners;

    private final ExecutorService                              _listenerExecutor;

    private final ExecutorService                              _jmDNSExecutor;

    private final Timer                                        _timer;

    private final AtomicBoolean                                _isClosing;

    private final AtomicBoolean                                _closed;

    /**
     * A semaphore to ensure LTE support is enabled only once.
     */
    private AtomicBoolean                                       lteSupportEnabled;

    /**
     * JmmDNSonLTE object
     */
    private JmmDNSonLTE                                         jmmDNSonLTE;


    /**
     *
     */
    public JmmDNSImpl() {
        super();
        _networkListeners = Collections.synchronizedSet(new HashSet<NetworkTopologyListener>());
        _knownMDNS = new ConcurrentHashMap<InetAddress, JmDNS>();
        _services = new ConcurrentHashMap<String, ServiceInfo>(20);
        _listenerExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("JmmDNS Listeners"));
        _jmDNSExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("JmmDNS"));
        _timer = new Timer("Multihomed mDNS.Timer", true);
        _serviceListeners = new ConcurrentHashMap<String, List<ServiceListener>>();
        _typeListeners = Collections.synchronizedSet(new HashSet<ServiceTypeListener>());
        _serviceTypes = Collections.synchronizedSet(new HashSet<String>());
        NetworkTopologyDiscovery topology = NetworkTopologyDiscovery.Factory.getInstance();
        (new NetworkChecker(this, topology)).start(_timer);
        _isClosing = new AtomicBoolean(false);
        _closed = new AtomicBoolean(false);
        lteSupportEnabled = new AtomicBoolean(false);

    }

    /*
     * (non-Javadoc)
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException {
        if (_isClosing.compareAndSet(false, true)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Cancelling JmmDNS: " + this);
            }
            _timer.cancel();
            _listenerExecutor.shutdown();
            _jmDNSExecutor.shutdown();
            // We need to cancel all the DNS
            ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("JmmDNS.close"));
            try {
                for (final JmDNS mDNS : this.getDNS()) {
                    executor.submit(new Runnable() {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void run() {
                            try {
                                mDNS.close();
                            } catch (IOException exception) {
                                // JmDNS never throws this is only because of the closeable interface
                            }
                        }
                    });
                }
            } finally {
                executor.shutdown();
            }
            try {
                executor.awaitTermination(DNSConstants.CLOSE_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                logger.warn("Exception ", exception);
            }
            _knownMDNS.clear();
            _services.clear();
            _serviceListeners.clear();
            _typeListeners.clear();
            _serviceTypes.clear();
            _closed.set(true);
            JmmDNS.Factory.close();
        }

        //close jmmDNSonLTE
        try {
            if (this.lteSupportEnabled.get() && this.jmmDNSonLTE != null) {
                this.jmmDNSonLTE.interrupt();
            }
        }catch (Exception e){
            logger.debug("JmmDNSImpl.close() exception: ", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getNames()
     */
    @Override
    public String[] getNames() {
        Set<String> result = new HashSet<String>();
        for (JmDNS mDNS : this.getDNS()) {
            result.add(mDNS.getName());
        }
        return result.toArray(new String[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getHostNames()
     */
    @Override
    public String[] getHostNames() {
        Set<String> result = new HashSet<String>();
        for (JmDNS mDNS : this.getDNS()) {
            result.add(mDNS.getHostName());
        }
        return result.toArray(new String[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getInetAddresses()
     */
    @Override
    public InetAddress[] getInetAddresses() throws IOException {
        Set<InetAddress> result = new HashSet<InetAddress>();
        for (JmDNS mDNS : this.getDNS()) {
            result.add(mDNS.getInetAddress());
        }
        return result.toArray(new InetAddress[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getDNS()
     */
    @Override
    public JmDNS[] getDNS() {
        synchronized (_knownMDNS) {
            return _knownMDNS.values().toArray(new JmDNS[_knownMDNS.size()]);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getInterfaces()
     */
    @Override
    @Deprecated
    public InetAddress[] getInterfaces() throws IOException {
        Set<InetAddress> result = new HashSet<InetAddress>();
        for (JmDNS mDNS : this.getDNS()) {
            result.add(mDNS.getInterface());
        }
        return result.toArray(new InetAddress[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getServiceInfos(java.lang.String, java.lang.String)
     */
    @Override
    public ServiceInfo[] getServiceInfos(String type, String name) {
        return this.getServiceInfos(type, name, false, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getServiceInfos(java.lang.String, java.lang.String, long)
     */
    @Override
    public ServiceInfo[] getServiceInfos(String type, String name, long timeout) {
        return this.getServiceInfos(type, name, false, timeout);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getServiceInfos(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public ServiceInfo[] getServiceInfos(String type, String name, boolean persistent) {
        return this.getServiceInfos(type, name, persistent, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getServiceInfos(java.lang.String, java.lang.String, boolean, long)
     */
    @Override
    public ServiceInfo[] getServiceInfos(final String type, final String name, final boolean persistent, final long timeout) {
        // We need to run this in parallel to respect the timeout.
        final JmDNS[] dnsArray = this.getDNS();
        final Set<ServiceInfo> result = new HashSet<ServiceInfo>(dnsArray.length);
        if (dnsArray.length > 0) {
            List<Callable<ServiceInfo>> tasks = new ArrayList<Callable<ServiceInfo>>(dnsArray.length);
            for (final JmDNS mDNS : dnsArray) {
                tasks.add(new Callable<ServiceInfo>() {

                    @Override
                    public ServiceInfo call() throws Exception {
                        return mDNS.getServiceInfo(type, name, persistent, timeout);
                    }

                });
            }

            ExecutorService executor = Executors.newFixedThreadPool(tasks.size(), new NamedThreadFactory("JmmDNS.getServiceInfos"));
            try {
                List<Future<ServiceInfo>> results = Collections.emptyList();
                try {
                    results = executor.invokeAll(tasks, timeout + 100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    logger.debug("Interrupted ", exception);
                    Thread.currentThread().interrupt();
                    // Will terminate next loop early.
                }

                for (Future<ServiceInfo> future : results) {
                    if (future.isCancelled()) {
                        continue;
                    }
                    try {
                        ServiceInfo info = future.get();
                        if (info != null) {
                            result.add(info);
                        }
                    } catch (InterruptedException exception) {
                        logger.debug("Interrupted ", exception);
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException exception) {
                        logger.warn("Exception ", exception);
                    }
                }
            } finally {
                executor.shutdown();
            }
        }
        return result.toArray(new ServiceInfo[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#requestServiceInfo(java.lang.String, java.lang.String)
     */
    @Override
    public void requestServiceInfo(String type, String name) {
        this.requestServiceInfo(type, name, false, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#requestServiceInfo(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void requestServiceInfo(String type, String name, boolean persistent) {
        this.requestServiceInfo(type, name, persistent, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#requestServiceInfo(java.lang.String, java.lang.String, long)
     */
    @Override
    public void requestServiceInfo(String type, String name, long timeout) {
        this.requestServiceInfo(type, name, false, timeout);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#requestServiceInfo(java.lang.String, java.lang.String, boolean, long)
     */
    @Override
    public void requestServiceInfo(final String type, final String name, final boolean persistent, final long timeout) {
        // We need to run this in parallel to respect the timeout.
        for (final JmDNS mDNS : this.getDNS()) {
            _jmDNSExecutor.submit(new Runnable() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run() {
                    mDNS.requestServiceInfo(type, name, persistent, timeout);
                }
            });
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#addServiceTypeListener(javax.jmdns.ServiceTypeListener)
     */
    @Override
    public void addServiceTypeListener(ServiceTypeListener listener) throws IOException {
        _typeListeners.add(listener);
        for (JmDNS mDNS : this.getDNS()) {
            mDNS.addServiceTypeListener(listener);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#removeServiceTypeListener(javax.jmdns.ServiceTypeListener)
     */
    @Override
    public void removeServiceTypeListener(ServiceTypeListener listener) {
        _typeListeners.remove(listener);
        for (JmDNS mDNS : this.getDNS()) {
            mDNS.removeServiceTypeListener(listener);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#addServiceListener(java.lang.String, javax.jmdns.ServiceListener)
     */
    @Override
    public void addServiceListener(String type, ServiceListener listener) {
        final String loType = type.toLowerCase();
        List<ServiceListener> list = _serviceListeners.get(loType);
        if (list == null) {
            _serviceListeners.putIfAbsent(loType, new LinkedList<ServiceListener>());
            list = _serviceListeners.get(loType);
        }
        if (list != null) {
            synchronized (list) {
                if (!list.contains(listener)) {
                    list.add(listener);
                }
            }
        }
        for (JmDNS mDNS : this.getDNS()) {
            mDNS.addServiceListener(type, listener);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#removeServiceListener(java.lang.String, javax.jmdns.ServiceListener)
     */
    @Override
    public void removeServiceListener(String type, ServiceListener listener) {
        String loType = type.toLowerCase();
        List<ServiceListener> list = _serviceListeners.get(loType);
        if (list != null) {
            synchronized (list) {
                list.remove(listener);
                if (list.isEmpty()) {
                    _serviceListeners.remove(loType, list);
                }
            }
        }
        for (JmDNS mDNS : this.getDNS()) {
            mDNS.removeServiceListener(type, listener);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.ServiceInfoImpl.Delegate#textValueUpdated(javax.jmdns.ServiceInfo, byte[])
     */
    @Override
    public void textValueUpdated(ServiceInfo target, byte[] value) {
        // We need to get the list out of the synchronized block to prevent dead locks
        final JmDNS[] dnsArray = this.getDNS();
        synchronized (_services) {
            for (JmDNS mDNS : dnsArray) {
                ServiceInfo info = ((JmDNSImpl) mDNS).getServices().get(target.getQualifiedName());
                if (info != null) {
                    info.setText(value);
                } else {
                    logger.warn("We have a mDNS that does not know about the service info being updated.");
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#registerService(javax.jmdns.ServiceInfo)
     */
    @Override
    public void registerService(ServiceInfo info) throws IOException {
        // We need to get the list out of the synchronized block to prevent dead locks
        final JmDNS[] dnsArray = this.getDNS();
        // This is really complex. We need to clone the service info for each DNS but then we loose the ability to update it.
        synchronized (_services) {
            for (JmDNS mDNS : dnsArray) {
                mDNS.registerService(info.clone());
            }
            ((ServiceInfoImpl) info).setDelegate(this);
            _services.put(info.getQualifiedName(), info);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#unregisterService(javax.jmdns.ServiceInfo)
     */
    @Override
    public void unregisterService(ServiceInfo info) {
        // We need to get the list out of the synchronized block to prevent dead locks
        final JmDNS[] dnsArray = this.getDNS();
        synchronized (_services) {
            _services.remove(info.getQualifiedName());
            for (JmDNS mDNS : dnsArray) {
                mDNS.unregisterService(info);
            }
            ((ServiceInfoImpl) info).setDelegate(null);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#unregisterAllServices()
     */
    @Override
    public void unregisterAllServices() {
        // We need to get the list out of the synchronized block to prevent dead locks
        final JmDNS[] dnsArray = this.getDNS();
        synchronized (_services) {
            _services.clear();
            for (JmDNS mDNS : dnsArray) {
                mDNS.unregisterAllServices();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#registerServiceType(java.lang.String)
     */
    @Override
    public void registerServiceType(String type) {
        _serviceTypes.add(type);
        for (JmDNS mDNS : this.getDNS()) {
            mDNS.registerServiceType(type);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#list(java.lang.String)
     */
    @Override
    public ServiceInfo[] list(String type) {
        return this.list(type, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#list(java.lang.String, long)
     */
    @Override
    public ServiceInfo[] list(final String type, final long timeout) {
        final JmDNS[] dnsArray = this.getDNS();
        // We need to run this in parallel to respect the timeout.
        final Set<ServiceInfo> result = new HashSet<ServiceInfo>(dnsArray.length * 5);
        if (dnsArray.length > 0) {
            List<Callable<List<ServiceInfo>>> tasks = new ArrayList<Callable<List<ServiceInfo>>>(dnsArray.length);
            for (final JmDNS mDNS : dnsArray) {
                tasks.add(new Callable<List<ServiceInfo>>() {
                    @Override
                    public List<ServiceInfo> call() throws Exception {
                        return Arrays.asList(mDNS.list(type, timeout));
                    }
                });
            }

            ExecutorService executor = Executors.newFixedThreadPool(tasks.size(), new NamedThreadFactory("JmmDNS.list"));
            try {
                List<Future<List<ServiceInfo>>> results = Collections.emptyList();
                try {
                    results = executor.invokeAll(tasks, timeout + 100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    logger.debug("Interrupted ", exception);
                    Thread.currentThread().interrupt();
                    // Will terminate next loop early.
                }

                for (Future<List<ServiceInfo>> future : results) {
                    if (future.isCancelled()) {
                        continue;
                    }
                    try {
                        result.addAll(future.get());
                    } catch (InterruptedException exception) {
                        logger.debug("Interrupted ", exception);
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException exception) {
                        logger.warn("Exception ", exception);
                    }
                }
            } finally {
                executor.shutdown();
            }
        }
        return result.toArray(new ServiceInfo[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#listBySubtype(java.lang.String)
     */
    @Override
    public Map<String, ServiceInfo[]> listBySubtype(String type) {
        return this.listBySubtype(type, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#listBySubtype(java.lang.String, long)
     */
    @Override
    public Map<String, ServiceInfo[]> listBySubtype(final String type, final long timeout) {
        Map<String, List<ServiceInfo>> map = new HashMap<String, List<ServiceInfo>>(5);
        for (ServiceInfo info : this.list(type, timeout)) {
            String subtype = info.getSubtype();
            if (!map.containsKey(subtype)) {
                map.put(subtype, new ArrayList<ServiceInfo>(10));
            }
            map.get(subtype).add(info);
        }

        Map<String, ServiceInfo[]> result = new HashMap<String, ServiceInfo[]>(map.size());
        for (String subtype : map.keySet()) {
            List<ServiceInfo> infoForSubType = map.get(subtype);
            result.put(subtype, infoForSubType.toArray(new ServiceInfo[infoForSubType.size()]));
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#addNetworkTopologyListener(javax.jmdns.NetworkTopologyListener)
     */
    @Override
    public void addNetworkTopologyListener(NetworkTopologyListener listener) {
        _networkListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#removeNetworkTopologyListener(javax.jmdns.NetworkTopologyListener)
     */
    @Override
    public void removeNetworkTopologyListener(NetworkTopologyListener listener) {
        _networkListeners.remove(listener);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#networkListeners()
     */
    @Override
    public NetworkTopologyListener[] networkListeners() {
        return _networkListeners.toArray(new NetworkTopologyListener[_networkListeners.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.NetworkTopologyListener#inetAddressAdded(javax.jmdns.NetworkTopologyEvent)
     */
    @Override
    public void inetAddressAdded(NetworkTopologyEvent event) {
        InetAddress address = event.getInetAddress();
        try {
            if (!_knownMDNS.containsKey(address)) {
                synchronized (_knownMDNS) {
                    if (!_knownMDNS.containsKey(address)) {
                        final JmDNS dns = JmDNS.create(address);
                        logger.info("inet_address_added: " + address.toString());

                        //if there was no value previously
                        if (_knownMDNS.putIfAbsent(address, dns) == null) {

                            // We need to register the services and listeners with the new JmDNS
                            final Collection<String> types = _serviceTypes;
                            final Collection<ServiceInfo> infos = _services.values();
                            final Collection<ServiceTypeListener> typeListeners = _typeListeners;
                            final Map<String, List<ServiceListener>> serviceListeners = _serviceListeners;

                            _jmDNSExecutor.submit(new Runnable() {
                                /**
                                 * {@inheritDoc}
                                 */
                                @Override
                                public void run() {

                                    // Register all Types for this IP
                                    for (String type : types) {
                                        dns.registerServiceType(type);
                                    }
                                    // Register all services for this IP
                                    for (ServiceInfo info : infos) {
                                        try {
                                            dns.registerService(info.clone());
                                        } catch (IOException exception) {
                                            // logger.warn("Unexpected unhandled exception: " + exception);
                                        }
                                    }
                                    // Add all ServiceType Listeners for this IP
                                    for (ServiceTypeListener listener : typeListeners) {
                                        try {
                                            dns.addServiceTypeListener(listener);
                                        } catch (IOException exception) {
                                            // logger.warn("Unexpected unhandled exception: " + exception);
                                        }
                                    }
                                    // Add all Service Listeners for this IP
                                    for (String type : serviceListeners.keySet()) {
                                        List<ServiceListener> listeners = serviceListeners.get(type);
                                        synchronized (listeners) {
                                            for (ServiceListener listener : listeners) {
                                                dns.addServiceListener(type, listener);
                                            }
                                        }
                                    }
                                }
                            });

                            final NetworkTopologyEvent jmdnsEvent = new NetworkTopologyEventImpl(dns, address);
                            for (final NetworkTopologyListener listener : this.networkListeners()) {
                                _listenerExecutor.submit(new Runnable() {
                                    /**
                                     * {@inheritDoc}
                                     */
                                    @Override
                                    public void run() {
                                        listener.inetAddressAdded(jmdnsEvent);
                                    }
                                });
                            }
                        } else {
                            dns.close();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Unexpected unhandled exception: " + e);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.NetworkTopologyListener#inetAddressRemoved(javax.jmdns.NetworkTopologyEvent)
     */
    @Override
    public void inetAddressRemoved(NetworkTopologyEvent event) {
        InetAddress address = event.getInetAddress();
        try {
            if (_knownMDNS.containsKey(address)) {
                synchronized (_knownMDNS) {
                    if (_knownMDNS.containsKey(address)) {
                        JmDNS mDNS = _knownMDNS.remove(address);
                        mDNS.close();
                        final NetworkTopologyEvent jmdnsEvent = new NetworkTopologyEventImpl(mDNS, address);
                        for (final NetworkTopologyListener listener : this.networkListeners()) {
                            _listenerExecutor.submit(new Runnable() {
                                /**
                                 * {@inheritDoc}
                                 */
                                @Override
                                public void run() {
                                    listener.inetAddressRemoved(jmdnsEvent);
                                }
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Unexpected unhandled exception: " + e);
        }
    }

    /**
     * Checks the network state.<br/>
     * If the network change, this class will reconfigure the list of DNS do adapt to the new configuration.
     */
    static class NetworkChecker extends TimerTask {
        private static Logger                  logger1 = LoggerFactory.getLogger(NetworkChecker.class.getName());

        private final NetworkTopologyListener  _mmDNS;

        private final NetworkTopologyDiscovery _topology;

        private Set<InetAddress>               _knownAddresses;

        public NetworkChecker(NetworkTopologyListener mmDNS, NetworkTopologyDiscovery topology) {
            super();
            this._mmDNS = mmDNS;
            this._topology = topology;
            _knownAddresses = Collections.synchronizedSet(new HashSet<InetAddress>());
        }

        public void start(Timer timer) {
            // Run once up-front otherwise the list of servers will only appear after a delay.
            run();
            timer.schedule(this, DNSConstants.NETWORK_CHECK_INTERVAL, DNSConstants.NETWORK_CHECK_INTERVAL);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                InetAddress[] curentAddresses = _topology.getInetAddresses();
                Set<InetAddress> current = new HashSet<InetAddress>(curentAddresses.length);
                for (InetAddress address : curentAddresses) {
                    current.add(address);
                    if (!_knownAddresses.contains(address)) {
                        final NetworkTopologyEvent event = new NetworkTopologyEventImpl(_mmDNS, address);
                        _mmDNS.inetAddressAdded(event);
                    }
                }
                for (InetAddress address : _knownAddresses) {
                    if (!current.contains(address)) {
                        final NetworkTopologyEvent event = new NetworkTopologyEventImpl(_mmDNS, address);
                        _mmDNS.inetAddressRemoved(event);
                    }
                }
                _knownAddresses = current;
            } catch (Exception e) {
                logger1.warn("Unexpected unhandled exception: " + e);
            }
        }

    }

    /**
     * Enables LTE support in JmmDNS.
     * Calling this function more than once should have no effect.
     * Once this function is called, it cannot be undone.
     */
    public void enableLTEsupport(ServiceInfo info, ServiceListener listener){
        if(!this.lteSupportEnabled.get()) {
            try {
                this.jmmDNSonLTE = new JmmDNSonLTE(info, listener);
                this.jmmDNSonLTE.start();
                this.lteSupportEnabled.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void disableLTEsupport(){
        if(this.lteSupportEnabled.get()){
            this.jmmDNSonLTE.interrupt();
            this.lteSupportEnabled.set(false);
        }
    }

    /****************************************************************************************************/

    /**
     * This class enables mDNS on LTE using UDP uni-cast.
     *
     * @author Ken Adams from Friends
     */
    private class JmmDNSonLTE extends Thread{

        /**
         * Log
         */
        Logger logger = LoggerFactory.getLogger(JmmDNSonLTE.class.getName());

        /**
         * Semaphore to ensure interrupt() function executes only once.
         */
        private boolean canCloseEverything = true;

        /**
         * Semaphore to signal application level that a service is added
         */
        private boolean serviceAddedSignaled = false;

        /**
         * Semaphore to signal application level that a service is removed
         */
        private boolean serviceRemovedSignaled = false;

        /**
         * Semaphore to ensure run().while loop keeps running
         */
        private boolean isWhileLoopRunning;


        /**
         * Sleep interval for run().while loop
         */
        private int sleepInterval = 10 * 1000;

        /**
         * Amount of time we wait before declaring other devices are gone
         */
        private int serviceRemoveWaitInterval = 30 * 1000;


        /**
         * Datagram socket through which all UDP are sent
         */
        DatagramSocket socket;

        /**
         * Ran in a separate thread to receive UDP packets from other nodes
         */
        Receiver receiver;

        /**
         * ServiceListener object with callback functions implemented by application level
         */
        ServiceListener listener;

        /**
         * A dummy jmdns object
         */
        JmDNSImpl dummyJmDNSImpl;

        /**
         * ServiceInfo object
         */
        ServiceInfo info;

        /**
         * An unique sequence ID that represent this device
         */
        String UniqueSeqID;

        /**
         * A list of sequence IDs of other devices
         */
        Map<String, LTEPacket> allDiscoveredSeqIDs;

        /**
         * An array to contain this node's LTE IP, subnet mask etc.
         */
        String[] LTEipAndMask;


        /**
         * Public Constructor only to be instantiated once inside JmmDNSImpl class
         * @param info
         * @param listener
         */
        public JmmDNSonLTE(ServiceInfo info, ServiceListener listener){
            this.isWhileLoopRunning = true;
            this.info = info;
            this.listener = listener;
            this.UniqueSeqID = new String();
            this.allDiscoveredSeqIDs = new ConcurrentHashMap<String, LTEPacket>();
        }


        @Override
        public void run(){

            ServiceEvent dummyEvent;
            ServiceInfo inf;

            //this loop runs perpetually
            while(isWhileLoopRunning){

                try {

                    //check if this device has a LTE interface
                    this.LTEipAndMask = getLTEipAndSubnet();

                    //this node has an LTE interface
                    if (this.LTEipAndMask != null) {

                        //when LTE ip is available for the first time, create a dummy JmDNSImpl object and dummy event for once only
                        if(this.dummyJmDNSImpl==null) {
                            this.dummyJmDNSImpl = new JmDNSImpl(InetAddress.getByName("255.255.255.255"), "dummy_name");
                        }

                        //mark as false so that when this service will be removed, application level gets notified
                        this.serviceRemovedSignaled = false;

                        //check if a socket is active in this LTE IP
                        if (this.socket != null && !this.socket.isClosed() && this.socket.getLocalAddress().getHostAddress().equals(this.LTEipAndMask[0])) {

                            //send UDP uni-cast
                            new Sender(this.socket, ServiceStatus.Status.SERVICE_ADDED, this.LTEipAndMask, this.info, this.UniqueSeqID).start();

                            //signal the application later once about a new service been added
                            if(!this.serviceAddedSignaled && this.dummyJmDNSImpl!=null) {

                                //notify application level about newly added service
                                dummyEvent = new ServiceEventImpl(dummyJmDNSImpl, this.info.getName(), this.info.getName(), this.info);
                                this.listener.serviceAdded(dummyEvent);

                                //mark as true so that application level is not signaled about same service multiple times
                                this.serviceAddedSignaled = true;
                            }

                        } else {

                            //Coming here means this.socket is invalid.
                            //LTE interface may or may not exists.
                            try {
                                //close socket and receiver
                                this.closeSocket();
                                this.closeReceiver();

                                //open new socket
                                openNewSocket(this.LTEipAndMask[0], DNSConstants.MDNS_LTE_PORT);

                                //generate and add a new sequence ID
                                this.UniqueSeqID = UUID.randomUUID().toString().substring(0, 12);

                                //initialize new Receiver object
                                this.receiver = new Receiver(this.socket, this.listener, this.dummyJmDNSImpl);
                                this.receiver.start();

                                //mark as false to notify application level about new service when socket becomes valid
                                this.serviceAddedSignaled = false;

                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                        }

                    } else {

                        //LTE interface vanished or unavailable.
                        //Close the socket and receiver
                        this.closeSocket();
                        this.closeReceiver();

                        //Service Removed coz lte interface vanished or does not exist
                        //only call listener when not already done so and dummyJmDNSImpl is not null.
                        if (!this.serviceRemovedSignaled && this.dummyJmDNSImpl!=null) {

                            //notify application level about recently removed service
                            dummyEvent = new ServiceEventImpl(dummyJmDNSImpl, this.info.getName(), this.info.getName(), this.info);
                            this.listener.serviceRemoved(dummyEvent);

                            //mark as true that we notify application level only once.
                            this.serviceRemovedSignaled = true;
                        }

                    }


                }catch (Exception e){
                    e.printStackTrace();
                    logger.error("Exception in JmmDNSonLTE.run().while() ", e);
                }

                //do discovered sequence IDs cleanup
                synchronized (this.allDiscoveredSeqIDs) {
                    try {
                        for (Iterator<Map.Entry<String, LTEPacket>> it = this.allDiscoveredSeqIDs.entrySet().iterator(); it.hasNext(); ) {
                            Map.Entry<String, LTEPacket> entry = it.next();

                            if (System.currentTimeMillis() > entry.getValue().receiveTime + serviceRemoveWaitInterval) {

                                // notify application level for service removed
                                inf = ServiceInfo.create(entry.getValue().type, entry.getValue().name, 0, entry.getValue().text);
                                dummyEvent = new ServiceEventImpl(dummyJmDNSImpl, inf.getName(), inf.getName(), inf);
                                this.listener.serviceRemoved(dummyEvent);

                                //remove the entry from the map
                                it.remove();
                            }
                        }
                    }catch (Exception e){
                        //exception may happen due to concurrent modification of allDiscoveredSeqIDs map by two threads
                    }
                }

                //sleep for interrupt
                try {
                    sleep(sleepInterval);
                } catch (InterruptedException e) {
                    //coming here means interrupt() has been called and everything is closing
                    try { this.socket.close(); } catch (Exception ee) { }
                    try { this.receiver.close(); } catch (Exception ee) { }
                    this.isWhileLoopRunning = false;
                }

            }

            //coming here means this thread is about to finish
            this.closeSocket();
            this.closeReceiver();
        }


        /**
         * Closes JmmDNSonLTE, along with the Receiver.
         * Semaphore @canCloseEverything ensures this function executes once.
         * Note: In Android, must call this function from onStop().
         */
        @Override
        public synchronized void interrupt(){
            if(canCloseEverything) {

                //mark as false so that code inside interrupt() executes only once.
                canCloseEverything = false;

                //send UDP packets to all to announce goodbye
                if (this.socket != null && !this.socket.isClosed()) {

                    //create a Sender with SERVICE_REMOVED tag
                    Sender end = new Sender(this.socket, ServiceStatus.Status.SERVICE_REMOVED, this.LTEipAndMask, this.info, this.UniqueSeqID);

                    //start sender thread
                    end.start();

                    //make the caller thread wait 5 seconds until sender thread is done.
                    try {
                        end.join(5*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //signal service removed through listener
                //only notify if the dummyJmDNSImpl is not null
                if(this.dummyJmDNSImpl!=null) {
                    ServiceEvent dummyEvent = new ServiceEventImpl(dummyJmDNSImpl, this.info.getName(), this.info.getName(), this.info);
                    this.listener.serviceRemoved(dummyEvent);
                }

                //ensure the while loop stops
                this.isWhileLoopRunning = false;

                //Note: DO NOT call super.interrupt() here or no goodbye messages will be announced.
            }
        }

        /**
         * Close the socket.
         */
        public void closeSocket(){
            try{
                this.socket.close();
            }catch (Exception e){

            }
        }

        /**
         * Close the receiver.
         */
        public void closeReceiver(){
            try{
                this.receiver.close();
            }catch (Exception e){

            }
        }

        /**
         * Opens a new socket in the given IP and port
         */
        private void openNewSocket(String ip, int port){
            try {
                this.socket = new DatagramSocket(port, InetAddress.getByName(ip));
                this.socket.setReuseAddress(true);
                logger.info("Created UDP socket for " + this.socket.getLocalAddress().getHostAddress() + ":" + this.socket.getLocalPort());
            } catch (IOException e) {
                logger.warn("Problem in opening DataGram socket for "+ ip + " : "+ port, e);
            }

        }

        /**
         * returns LTE IP and its subnet mask.
         * returns an array of size two or null.
         * @return String[]
         */
        public String[] getLTEipAndSubnet() {
            try {
                Enumeration<NetworkInterface> enumNI = NetworkInterface.getNetworkInterfaces();
                while (enumNI.hasMoreElements()) {
                    NetworkInterface ifc = enumNI.nextElement();
                    if (ifc.isUp()) {
                        Enumeration<InetAddress> enumAdds = ifc.getInetAddresses();
                        while (enumAdds.hasMoreElements()) {
                            InetAddress addr = enumAdds.nextElement();
                            if (!(addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress())){
                                if (addr instanceof Inet4Address){
                                    if(ifc.getName().startsWith("rmnet") || ifc.getName().startsWith("pgwtun")){
                                        String[] LTEipAndMask = new String[2];
                                        LTEipAndMask[0] = addr.toString().substring(1);
                                        LTEipAndMask[1] = Short.toString(NetworkInterface.getByInetAddress(addr).getInterfaceAddresses().get(0).getNetworkPrefixLength());
                                        return LTEipAndMask;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (SocketException e) {
                logger.error("Problem in obtaining LTE IP and subnet for this device ", e);
            }
            return null;
        }

        //----------------------------------------------------------------------------------

        /**
         * Receives UDP packets from other nodes.
         */
        private class Receiver extends Thread{

            JmDNSImpl dummyJmDNSImpl;
            DatagramSocket socket;
            boolean isRunning;
            ServiceListener listener;



            private Receiver(DatagramSocket socket, ServiceListener listener, JmDNSImpl dummyJmDNSImpl){
                this.socket = socket;
                this.isRunning = true;
                this.listener = listener;
                this.dummyJmDNSImpl = dummyJmDNSImpl;
            }

            @Override
            public void run(){

                this.isRunning = true;
                byte buf[] = new byte[DNSConstants.MAX_MSG_ABSOLUTE];
                DatagramPacket packet;
                byte[] data;
                LTEPacket ltePacket;
                LTEPacket ltePacketTemp = new LTEPacket();

                while(isRunning){
                    try {

                        //make a new empty packet
                        packet = new DatagramPacket(buf, buf.length);

                        //if socket not closed
                        if(!this.socket.isClosed()) {

                            //block to receive UDP packet
                            socket.receive(packet);

                            //get data bytes from packet
                            data = packet.getData();

                            //convert Datagram packet into LTEPacket object
                            ltePacket = ltePacketTemp.BArrayToObject(data);
                            ltePacket.receiveTime = System.currentTimeMillis();

                            //if this is a new query message with unique seq ID
                            if(!allDiscoveredSeqIDs.keySet().contains(ltePacket.UniqueSeqID)){

                                if(ltePacket.status == ServiceStatus.Status.SERVICE_ADDED){

                                    //add this service to allDiscoveredSeqIDs
                                    allDiscoveredSeqIDs.put(ltePacket.UniqueSeqID, ltePacket);

                                    //notify application level that a new service is added
                                    if(this.dummyJmDNSImpl!=null) {

                                        //Service Added
                                        ServiceInfo inf = ServiceInfo.create(ltePacket.type, ltePacket.name, 0, ltePacket.text);
                                        ServiceEvent dummyEvent = new ServiceEventImpl(dummyJmDNSImpl, inf.getName(), inf.getName(), inf);
                                        this.listener.serviceAdded(dummyEvent);

                                        //sleep to mimic that added service is being resolved
                                        Thread.sleep(1000);

                                        //Service Resolved
                                        this.listener.serviceResolved(dummyEvent);
                                    }

                                }else if(ltePacket.status== ServiceStatus.Status.SERVICE_REMOVED){

                                    //do nothing because this service was never added in allDiscoveredSeqIDs at the first place.
                                }

                            }else{

                                if(ltePacket.status == ServiceStatus.Status.SERVICE_ADDED){

                                    //update the latest discovery time for this sequence ID
                                    allDiscoveredSeqIDs.put(ltePacket.UniqueSeqID, ltePacket);

                                }else if(ltePacket.status== ServiceStatus.Status.SERVICE_REMOVED){

                                    //Service Removed coz other node closed app etc.
                                    if(this.dummyJmDNSImpl!=null) {

                                        //notify application level for service removed
                                        ServiceInfo inf = ServiceInfo.create(ltePacket.type, ltePacket.name, 0, ltePacket.text);
                                        ServiceEvent dummyEvent = new ServiceEventImpl(dummyJmDNSImpl, inf.getName(), inf.getName(), inf);
                                        this.listener.serviceRemoved(dummyEvent);
                                    }

                                    //remove all seq IDs of this packet from allDiscoveredSeqIDs list
                                    allDiscoveredSeqIDs.remove(ltePacket.UniqueSeqID);
                                }
                            }

                        }else{
                            //socket is closed so break out of loop
                            try{this.socket.close();}catch (Exception e){}
                            this.isRunning = false;
                            break;
                        }

                        //sleep for interrupt
                        try { Thread.sleep(0); } catch (InterruptedException e) { e.printStackTrace(); }


                    }catch (Exception e){
                    }
                }
            }


            public void close() {
                try {
                    this.isRunning = false;
                    this.interrupt();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        //----------------------------------------------------------------------------------

        /**
         * Sends UDP packets to all class C IPs,
         * and sends good-bye messages when JmmDNSonLTE is closed.
         */
        public class Sender extends Thread{

            Logger logger = LoggerFactory.getLogger(Sender.class.getName());

            DatagramSocket socket;
            ServiceStatus.Status status;
            String[] LTEipAndMask;
            ServiceInfo info;
            String UniqueSeqID;

            public Sender(DatagramSocket socket, ServiceStatus.Status status, String[] LTEipAndMask, ServiceInfo info, String UniqueSeqID){
                this.socket = socket;
                this.status = status;
                this.LTEipAndMask = LTEipAndMask;
                this.info = info;
                this.UniqueSeqID = UniqueSeqID;
            }

            @Override
            public void run(){

                try{

                    //if this node has an LTE interface and class C IP
                    if(this.LTEipAndMask!=null && Integer.parseInt(this.LTEipAndMask[1])>=24){

                        //socket is still alive so send probe in it
                        //create new probing packet
                        LTEPacket ltePacket = new LTEPacket(this.info.getName(), this.info.getType(), new String(this.info.getTextBytes()), this.status, this.UniqueSeqID);

                        //Convert object to ByteArray
                        byte[] ltePacketArray = ltePacket.objectToBArray(ltePacket);

                        //convert byteArray to DatagramPacket
                        DatagramPacket packet = new DatagramPacket(ltePacketArray, 0, ltePacketArray.length);

                        //send broadcast from ip range 2 to 254
                        String destIPprefix = this.LTEipAndMask[0].substring(0, this.LTEipAndMask[0].lastIndexOf("."));

                        for(int i=2; i<254; i++){

                            //prepare destIP as String
                            String destIP = destIPprefix + "." + i;

                            if(!destIP.equals(LTEipAndMask[0])) {

                                //convert destIP into destAddr
                                InetAddress destAddr = InetAddress.getByName(destIP);

                                //set destAddr and port into packet
                                packet.setAddress(destAddr);
                                packet.setPort(DNSConstants.MDNS_LTE_PORT);

                                //send
                                this.socket.send(packet);
                            }
                        }

                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        //----------------------------------------------------------------------------------
    }
}