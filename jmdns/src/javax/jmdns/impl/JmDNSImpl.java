///Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;
import javax.jmdns.impl.tasks.Announcer;
import javax.jmdns.impl.tasks.Canceler;
import javax.jmdns.impl.tasks.Prober;
import javax.jmdns.impl.tasks.RecordReaper;
import javax.jmdns.impl.tasks.Renewer;
import javax.jmdns.impl.tasks.Responder;
import javax.jmdns.impl.tasks.ServiceInfoResolver;
import javax.jmdns.impl.tasks.ServiceResolver;
import javax.jmdns.impl.tasks.TypeResolver;

// REMIND: multiple IP addresses

/**
 * mDNS implementation in Java.
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Rick Blair, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Scott Lewis
 */
public class JmDNSImpl extends JmDNS
{
    private static Logger logger = Logger.getLogger(JmDNSImpl.class.getName());

    /**
     * This is the multicast group, we are listening to for multicast DNS messages.
     */
    private volatile InetAddress _group;
    /**
     * This is our multicast socket.
     */
    private volatile MulticastSocket _socket;

    /**
     * Used to fix live lock problem on unregister.
     */
    private volatile boolean _closed = false;

    /**
     * Holds instances of JmDNS.DNSListener. Must by a synchronized collection, because it is updated from concurrent threads.
     */
    private final List<DNSListener> _listeners;

    /**
     * Holds instances of ServiceListener's. Keys are Strings holding a fully qualified service type. Values are LinkedList's of ServiceListener's.
     */
    private final ConcurrentMap<String, List<ServiceListener>> _serviceListeners;

    /**
     * Holds instances of ServiceTypeListener's.
     */
    private final Set<ServiceTypeListener> _typeListeners;

    /**
     * Cache for DNSEntry's.
     */
    private DNSCache _cache;

    /**
     * This hashtable holds the services that have been registered. Keys are instances of String which hold an all lower-case version of the fully qualified service name. Values are instances of ServiceInfo.
     */
    private final ConcurrentMap<String, ServiceInfo> _services;

    /**
     * This hashtable holds the service types that have been registered or that have been received in an incoming datagram. Keys are instances of String which hold an all lower-case version of the fully qualified service type. Values hold the fully
     * qualified service type.
     */
    private final ConcurrentMap<String, String> _serviceTypes;
    /**
     * This is the shutdown hook, we registered with the java runtime.
     */
    protected Thread _shutdown;

    /**
     * Handle on the local host
     */
    private HostInfo _localHost;

    private final Thread _incomingListener;

    /**
     * Throttle count. This is used to count the overall number of probes sent by JmDNS. When the last throttle increment happened .
     */
    private int _throttle;
    /**
     * Last throttle increment.
     */
    private long _lastThrottleIncrement;

    //
    // 2009-09-16 ldeck: adding docbug patch with slight ammendments
    // 'Fixes two deadlock conditions involving JmDNS.close() - ID: 1473279'
    //
    // ---------------------------------------------------
    /**
     * The timer that triggers our announcements. We can't use the main timer object, because that could cause a deadlock where Prober waits on JmDNS.this lock held by close(), close() waits for us to finish, and we wait for Prober to give us back
     * the timer thread so we can announce. (Patch from docbug in 2006-04-19 still wasn't patched .. so I'm doing it!)
     */
    private Timer _cancelerTimer = new Timer("JmDNS.cancelerTimer");
    // ---------------------------------------------------

    /**
     * The timer is used to dispatch all outgoing messages of JmDNS. It is also used to dispatch maintenance tasks for the DNS cache.
     */
    private final Timer _timer;

    /**
     * The source for random values. This is used to introduce random delays in responses. This reduces the potential for collisions on the network.
     */
    private final static Random _random = new Random();

    /**
     * This lock is used to coordinate processing of incoming and outgoing messages. This is needed, because the Rendezvous Conformance Test does not forgive race conditions.
     */
    private final ReentrantLock _ioLock = new ReentrantLock();

    /**
     * If an incoming package which needs an answer is truncated, we store it here. We add more incoming DNSRecords to it, until the JmDNS.Responder timer picks it up. Remind: This does not work well with multiple planned answers for packages that
     * came in from different clients.
     */
    private DNSIncoming _plannedAnswer;

    // State machine
    /**
     * The state of JmDNS.
     * <p/>
     * For proper handling of concurrency, this variable must be changed only using methods advanceState(), revertState() and cancel().
     */
    private DNSState _state = DNSState.PROBING_1;

    /**
     * Timer task associated to the host name. This is used to prevent from having multiple tasks associated to the host name at the same time.
     */
    private TimerTask _task;

    /**
     * This hashtable is used to maintain a list of service types being collected by this JmDNS instance. The key of the hashtable is a service type name, the value is an instance of JmDNS.ServiceCollector.
     *
     * @see #list
     */
    private final ConcurrentMap<String, ServiceCollector> _serviceCollectors;

    private final String _name;

    /**
     * Create an instance of JmDNS and bind it to a specific network interface given its IP-address.
     *
     * @param address
     *            IP address to bind to.
     * @param name
     *            name of the newly created JmDNS
     * @throws IOException
     */
    public JmDNSImpl(InetAddress address, String name) throws IOException
    {
        super();
        logger.finer("JmDNS instance created");
        _cache = new DNSCache(100);

        _listeners = Collections.synchronizedList(new ArrayList<DNSListener>());
        _serviceListeners = new ConcurrentHashMap<String, List<ServiceListener>>();
        _typeListeners = Collections.synchronizedSet(new HashSet<ServiceTypeListener>());
        _serviceCollectors = new ConcurrentHashMap<String, ServiceCollector>();

        _services = new ConcurrentHashMap<String, ServiceInfo>(20);
        _serviceTypes = new ConcurrentHashMap<String, String>(20);

        _timer = new Timer("JmDNS.Timer");
        new RecordReaper(this).start(_timer);

        // (ldeck 2.1.1) preventing shutdown blocking thread
        // -------------------------------------------------
        // _shutdown = new Thread(new Shutdown(), "JmDNS.Shutdown");
        // Runtime.getRuntime().addShutdownHook(_shutdown);

        _incomingListener = new Thread(new SocketListener(this), "JmDNS.SocketListener");
        _incomingListener.setDaemon(true);
        // -------------------------------------------------

        try
        {
            InetAddress addr = address;
            String aName = "";
            if (addr == null)
            {
                addr = InetAddress.getLocalHost();
                aName = addr.getHostName();
                // [PJYF Oct 14 2004] Why do we disallow the loopback address ?
                if (addr.isLoopbackAddress())
                {
                    addr = null;
                }
            }
            else
            {
                aName = addr.getHostName();
            }
            // A host name with "." is illegal. so strip off everything and append .local.
            final int idx = aName.indexOf(".");
            if (idx > 0)
            {
                aName = aName.substring(0, idx);
            }
            aName += ".local.";
            _localHost = new HostInfo(addr, aName);
            // Bind to multicast socket
            this.openMulticastSocket(this.getLocalHost());
            this.start(this.getServices().values());
        }
        catch (final IOException e)
        {
            // FIXME [PJYF Dec 17 2009] This looks really bizarre why not fail and throw an exception. What good will this provide?
            _localHost = new HostInfo(null, "computer");
            // Bind to multicast socket
            this.openMulticastSocket(this.getLocalHost());
            this.start(this.getServices().values());
        }
        _name = (name != null ? name : _localHost.getName());
    }

    private void start(Collection<? extends ServiceInfo> serviceInfos)
    {
        this.setState(DNSState.PROBING_1);
        _incomingListener.start();
        new Prober(this).start(_timer);
        for (ServiceInfo info : serviceInfos)
        {
            try
            {
                this.registerService(new ServiceInfoImpl(info));
            }
            catch (final Exception exception)
            {
                logger.log(Level.WARNING, "start() Registration exception ", exception);
            }
        }
    }

    private void openMulticastSocket(HostInfo hostInfo) throws IOException
    {
        if (_group == null)
        {
            _group = InetAddress.getByName(DNSConstants.MDNS_GROUP);
        }
        if (_socket != null)
        {
            this.closeMulticastSocket();
        }
        _socket = new MulticastSocket(DNSConstants.MDNS_PORT);
        if ((hostInfo != null) && (hostInfo.getInterface() != null))
        {
            _socket.setNetworkInterface(hostInfo.getInterface());
        }
        _socket.setTimeToLive(255);
        _socket.joinGroup(_group);
    }

    private void closeMulticastSocket()
    {
        // jP: 20010-01-18. See below. We'll need this monitor...
        assert (Thread.holdsLock(this));
        logger.finer("closeMulticastSocket()");
        if (_socket != null)
        {
            // close socket
            try
            {
                _socket.leaveGroup(_group);
                _socket.close();
                // jP: 20010-01-18. It isn't safe to join() on the listener
                // thread - it attempts to lock the IoLock object, and deadlock
                // ensues. Per issue #2933183, changed this to wait on the JmDNS
                // monitor, checking on each notify (or timeout) that the
                // listener thread has stopped.
                //
                while (_incomingListener != null && _incomingListener.isAlive())
                {
                    try
                    {
                        // wait time is arbitrary, we're really expecting notification.
                        logger.finer("closeMulticastSocket(): waiting for jmDNS monitor");
                        this.wait(1000);
                    }
                    catch (InterruptedException ignored)
                    {
                        // Ignored
                    }
                }
            }
            catch (final Exception exception)
            {
                logger.log(Level.WARNING, "closeMulticastSocket() Close socket exception ", exception);
            }
            _socket = null;
        }
    }

    // State machine
    /**
     * Sets the state and notifies all objects that wait on JmDNS.
     */
    public synchronized void advanceState()
    {
        this.setState(getState().advance());
        this.notifyAll();
    }

    /**
     * Sets the state and notifies all objects that wait on JmDNS.
     */
    synchronized void revertState()
    {
        this.setState(getState().revert());
        this.notifyAll();
    }

    /**
     * Sets the state and notifies all objects that wait on JmDNS.
     */
    synchronized void cancel()
    {
        this.setState(DNSState.CANCELED);
        this.notifyAll();
    }

    /**
     * Returns the current state of this info.
     *
     * @return Info state
     */
    public DNSState getState()
    {
        return _state;
    }

    /**
     * Return the DNSCache associated with the cache variable
     *
     * @return DNS cache
     */
    public DNSCache getCache()
    {
        return _cache;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#getName()
     */
    @Override
    public String getName()
    {
        return _name;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#getHostName()
     */
    @Override
    public String getHostName()
    {
        return _localHost.getName();
    }

    /**
     * Returns the local host info
     *
     * @return local host info
     */
    public HostInfo getLocalHost()
    {
        return _localHost;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#getInterface()
     */
    @Override
    public InetAddress getInterface() throws IOException
    {
        return _socket.getInterface();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#getServiceInfo(java.lang.String, java.lang.String)
     */
    @Override
    public ServiceInfo getServiceInfo(String type, String name)
    {
        return this.getServiceInfo(type, name, 5 * 1000);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#getServiceInfo(java.lang.String, java.lang.String, int)
     */
    @Override
    public ServiceInfo getServiceInfo(String type, String name, int timeout)
    {
        this.registerServiceType(type);
        String lotype = type.toLowerCase();
        if (_serviceCollectors.putIfAbsent(lotype, new ServiceCollector(lotype)) == null)
        {
            this.addServiceListener(lotype, _serviceCollectors.get(lotype));
        }

        // Check if the answer is in the cache.
        final ServiceInfoImpl info = this.getServiceInfoFromCache(type, name);
        // We still run the resolver to do the dispatch but if the info is already there it will quit immediately
        new ServiceInfoResolver(this, info).start(_timer);

        this.waitForInfoData(info, timeout);

        return (info.hasData()) ? info : null;
    }

    private ServiceInfoImpl getServiceInfoFromCache(String type, String name)
    {
        // Check if the answer is in the cache.
        ServiceInfoImpl info = new ServiceInfoImpl(type, name);
        DNSEntry serviceEntry = this.getCache().getDNSEntry(info.getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_ANY);
        if (serviceEntry instanceof DNSRecord)
        {
            ServiceInfo cachedInfo = ((DNSRecord) serviceEntry).getServiceInfo();
            if (cachedInfo instanceof ServiceInfoImpl)
            {
                // To get a complete info record we need to retrieve the address and the text bytes.

                ServiceInfoImpl cachedInfoImp = (ServiceInfoImpl) cachedInfo;
                byte[] srvBytes = cachedInfoImp.getText();
                cachedInfoImp.setText(null);
                DNSEntry addressEntry = this.getCache().getDNSEntry(cachedInfo.getServer(), DNSRecordType.TYPE_A, DNSRecordClass.CLASS_ANY);
                if (addressEntry == null)
                {
                    addressEntry = this.getCache().getDNSEntry(cachedInfo.getServer(), DNSRecordType.TYPE_AAAA, DNSRecordClass.CLASS_ANY);
                }
                if (addressEntry instanceof DNSRecord)
                {
                    ServiceInfo cachedAddressInfo = ((DNSRecord) addressEntry).getServiceInfo();
                    if (cachedAddressInfo != null)
                    {
                        cachedInfoImp.setAddress(cachedAddressInfo.getAddress());
                        cachedInfoImp.setText(cachedAddressInfo.getTextBytes());
                    }
                }
                DNSEntry textEntry = this.getCache().getDNSEntry(cachedInfo.getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_ANY);
                if (textEntry instanceof DNSRecord)
                {
                    ServiceInfo cachedTextInfo = ((DNSRecord) textEntry).getServiceInfo();
                    if (cachedTextInfo != null)
                    {
                        cachedInfoImp.setText(cachedTextInfo.getTextBytes());
                    }
                }
                if (cachedInfoImp.getTextBytes().length == 0)
                {
                    cachedInfoImp.setText(srvBytes);
                }
                if (cachedInfoImp.hasData())
                {
                    info = cachedInfoImp;
                }
            }
        }
        return info;
    }

    private void waitForInfoData(ServiceInfo info, int timeout)
    {
        synchronized (info)
        {
            long loops = (timeout / 200L);
            if (loops < 1)
            {
                loops = 1;
            }
            for (int i = 0; i < loops; i++)
            {
                try
                {
                    info.wait(200);
                }
                catch (final InterruptedException e)
                {
                    /* Stub */
                }
                if (info.hasData())
                {
                    break;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#requestServiceInfo(java.lang.String, java.lang.String)
     */
    @Override
    public void requestServiceInfo(String type, String name)
    {
        this.requestServiceInfo(type, name, 5 * 1000);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#requestServiceInfo(java.lang.String, java.lang.String, int)
     */
    @Override
    public void requestServiceInfo(String type, String name, int timeout)
    {
        this.registerServiceType(type);
        String lotype = type.toLowerCase();
        if (_serviceCollectors.putIfAbsent(lotype, new ServiceCollector(lotype)) == null)
        {
            this.addServiceListener(lotype, _serviceCollectors.get(lotype));
        }

        // Check if the answer is in the cache.
        final ServiceInfoImpl info = this.getServiceInfoFromCache(type, name);
        // We still run the resolver to do the dispatch but if the info is already there it will quit immediately
        new ServiceInfoResolver(this, info).start(_timer);

        this.waitForInfoData(info, timeout);

    }

    void handleServiceResolved(ServiceInfoImpl info)
    {
        List<ServiceListener> list = _serviceListeners.get(info.getType().toLowerCase());
        List<ServiceListener> listCopy = Collections.emptyList();
        if ((list != null) && (!list.isEmpty()))
        {
            synchronized (list)
            {
                listCopy = new ArrayList<ServiceListener>(list);
            }
            final ServiceEvent event = new ServiceEventImpl(this, info.getType(), info.getName(), info);
            for (ServiceListener listener : listCopy)
            {
                listener.serviceResolved(event);
            }
        }
    }

    /**
     * @see javax.jmdns.JmDNS#addServiceTypeListener(javax.jmdns.ServiceTypeListener )
     */
    @Override
    public void addServiceTypeListener(ServiceTypeListener listener) throws IOException
    {
        _typeListeners.add(listener);

        // report cached service types
        for (String type : _serviceTypes.values())
        {
            listener.serviceTypeAdded(new ServiceEventImpl(this, type, null, null));
        }

        new TypeResolver(this).start(_timer);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#removeServiceTypeListener(javax.jmdns.ServiceTypeListener)
     */
    @Override
    public void removeServiceTypeListener(ServiceTypeListener listener)
    {
        _typeListeners.remove(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#addServiceListener(java.lang.String, javax.jmdns.ServiceListener)
     */
    @Override
    public void addServiceListener(String type, ServiceListener listener)
    {
        final String lotype = type.toLowerCase();
        List<ServiceListener> list = _serviceListeners.get(lotype);
        if (list == null)
        {
            if (_serviceListeners.putIfAbsent(lotype, new LinkedList<ServiceListener>()) == null)
            {
                if (_serviceCollectors.putIfAbsent(lotype, new ServiceCollector(lotype)) == null)
                {
                    this.addServiceListener(lotype, _serviceCollectors.get(lotype));
                }
            }
            list = _serviceListeners.get(lotype);
        }
        synchronized (list)
        {
            if (!list.contains(listener))
            {
                list.add(listener);
            }
        }

        // report cached service types
        final List<ServiceEvent> serviceEvents = new ArrayList<ServiceEvent>();
        Collection<DNSEntry> dnsEntryLits = this.getCache().allValues();
        for (DNSEntry entry : dnsEntryLits)
        {
            final DNSRecord record = (DNSRecord) entry;
            if (DNSRecordType.TYPE_SRV.equals(record.getRecordType()))
            {
                if (record.getName().endsWith(type))
                {
                    serviceEvents.add(new ServiceEventImpl(this, type, toUnqualifiedName(type, record.getName()), record.getServiceInfo()));
                }
            }
        }
        // Actually call listener with all service events added above
        for (ServiceEvent serviceEvent : serviceEvents)
        {
            listener.serviceAdded(serviceEvent);
        }
        // Create/start ServiceResolver
        new ServiceResolver(this, type).start(_timer);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#removeServiceListener(java.lang.String, javax.jmdns.ServiceListener)
     */
    @Override
    public void removeServiceListener(String type, ServiceListener listener)
    {
        String aType = type.toLowerCase();
        List<ServiceListener> list = _serviceListeners.get(aType);
        if (list != null)
        {
            synchronized (list)
            {
                list.remove(listener);
                if (list.isEmpty())
                {
                    _serviceListeners.remove(aType, list);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#registerService(javax.jmdns.ServiceInfo)
     */
    @Override
    public void registerService(ServiceInfo infoAbstract) throws IOException
    {
        final ServiceInfoImpl info = (ServiceInfoImpl) infoAbstract;

        this.registerServiceType(info.getType());

        // bind the service to this address
        info.setServer(_localHost.getName());
        info.setAddress(_localHost.getAddress());

        this.makeServiceNameUnique(info);
        while (_services.putIfAbsent(info.getQualifiedName().toLowerCase(), info) != null)
        {
            this.makeServiceNameUnique(info);
        }

        new /* Service */Prober(this).start(_timer);
        try
        {
            synchronized (info)
            {
                while (!info.getState().isAnnounced())
                {
                    info.wait();
                }
            }
        }
        catch (final InterruptedException e)
        {
            // empty
        }
        logger.fine("registerService() JmDNS registered service as " + info);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#unregisterService(javax.jmdns.ServiceInfo)
     */
    @Override
    public void unregisterService(ServiceInfo infoAbstract)
    {
        final ServiceInfoImpl info = (ServiceInfoImpl) infoAbstract;
        _services.remove(info.getQualifiedName().toLowerCase());
        info.cancel();

        // Note: We use this lock object to synchronize on it.
        // Synchronizing on another object (e.g. the ServiceInfo) does
        // not make sense, because the sole purpose of the lock is to
        // wait until the canceler has finished. If we synchronized on
        // the ServiceInfo or on the Canceler, we would block all
        // accesses to synchronized methods on that object. This is not
        // what we want!
        final Object lock = new Object();
        new Canceler(this, info, lock).start(_timer);

        // Remind: We get a deadlock here, if the Canceler does not run!
        try
        {
            synchronized (lock)
            {
                lock.wait();
            }
        }
        catch (final InterruptedException e)
        {
            // empty
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#unregisterAllServices()
     */
    @Override
    public void unregisterAllServices()
    {
        logger.finer("unregisterAllServices()");
        if (_services.size() == 0)
        {
            return;
        }

        List<ServiceInfo> list = new ArrayList<ServiceInfo>(_services.size());
        for (String name : _services.keySet())
        {
            ServiceInfo info = _services.get(name);
            if (info != null)
            {
                _services.remove(name, info);
                ((ServiceInfoImpl) info).cancel();
                list.add(info);
            }
        }

        final Object lock = new Object();
        //
        // 2009-09-16 ldeck: adding docbug patch with slight ammendments
        // 'Fixes two deadlock conditions involving JmDNS.close() - ID: 1473279'
        //
        // ---------------------------------------------------
        new Canceler(this, list, lock).start(_cancelerTimer);
        // new Canceler(this, list, lock).start(timer);
        // ---------------------------------------------------
        // Remind: We get a livelock here, if the Canceler does not run!
        try
        {
            synchronized (lock)
            {
                if (!_closed)
                {
                    lock.wait();
                }
            }
        }
        catch (final InterruptedException e)
        {
            // empty
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#registerServiceType(java.lang.String)
     */
    @Override
    public void registerServiceType(String type)
    {
        final String name = type.toLowerCase();
        if (!_serviceTypes.containsKey(name) && (name.indexOf("._mdns._udp.") < 0) && !name.endsWith(".in-addr.arpa."))
        {
            boolean typeAdded = _serviceTypes.putIfAbsent(name, type) == null;
            if (typeAdded)
            {
                ServiceTypeListener[] list = _typeListeners.toArray(new ServiceTypeListener[_typeListeners.size()]);
                for (ServiceTypeListener listener : list)
                {
                    listener.serviceTypeAdded(new ServiceEventImpl(this, type, null, null));
                }
            }
        }
    }

    /**
     * Generate a possibly unique name for a service using the information we have in the cache.
     *
     * @return returns true, if the name of the service info had to be changed.
     */
    private boolean makeServiceNameUnique(ServiceInfoImpl info)
    {
        final String originalQualifiedName = info.getQualifiedName();
        final long now = System.currentTimeMillis();

        boolean collision;
        do
        {
            collision = false;

            // Check for collision in cache
            Collection<? extends DNSEntry> entryList = this.getCache().getDNSEntryList(info.getQualifiedName().toLowerCase());
            if (entryList != null)
            {
                for (DNSEntry dnsEntry : entryList)
                {
                    if (DNSRecordType.TYPE_SRV.equals(dnsEntry.getRecordType()) && !dnsEntry.isExpired(now))
                    {
                        final DNSRecord.Service s = (DNSRecord.Service) dnsEntry;
                        if (s._port != info.getPort() || !s._server.equals(_localHost.getName()))
                        {
                            logger.finer("makeServiceNameUnique() JmDNS.makeServiceNameUnique srv collision:" + dnsEntry + " s.server=" + s._server + " " + _localHost.getName() + " equals:" + (s._server.equals(_localHost.getName())));
                            info.setName(incrementName(info.getName()));
                            collision = true;
                            break;
                        }
                    }
                }
            }

            // Check for collision with other service infos published by JmDNS
            final ServiceInfo selfService = _services.get(info.getQualifiedName().toLowerCase());
            if (selfService != null && selfService != info)
            {
                info.setName(incrementName(info.getName()));
                collision = true;
            }
        }
        while (collision);

        return !(originalQualifiedName.equals(info.getQualifiedName()));
    }

    String incrementName(String name)
    {
        String aName = name;
        try
        {
            final int l = aName.lastIndexOf('(');
            final int r = aName.lastIndexOf(')');
            if ((l >= 0) && (l < r))
            {
                aName = aName.substring(0, l) + "(" + (Integer.parseInt(aName.substring(l + 1, r)) + 1) + ")";
            }
            else
            {
                aName += " (2)";
            }
        }
        catch (final NumberFormatException e)
        {
            aName += " (2)";
        }
        return aName;
    }

    /**
     * Add a listener for a question. The listener will receive updates of answers to the question as they arrive, or from the cache if they are already available.
     *
     * @param listener
     *            DSN listener
     * @param question
     *            DNS query
     */
    public void addListener(DNSListener listener, DNSQuestion question)
    {
        final long now = System.currentTimeMillis();

        // add the new listener
        _listeners.add(listener);

        // report existing matched records

        if (question != null)
        {
            Collection<? extends DNSEntry> entryList = this.getCache().getDNSEntryList(question.getName().toLowerCase());
            if (entryList != null)
            {
                for (DNSEntry dnsEntry : entryList)
                {
                    if (question.answeredBy(dnsEntry) && !dnsEntry.isExpired(now))
                    {
                        listener.updateRecord(this.getCache(), now, dnsEntry);
                    }
                }
            }
        }
    }

    /**
     * Remove a listener from all outstanding questions. The listener will no longer receive any updates.
     *
     * @param listener
     *            DSN listener
     */
    public void removeListener(DNSListener listener)
    {
        _listeners.remove(listener);
    }

    // Remind: Method updateRecord should receive a better name.
    /**
     * Notify all listeners that a record was updated.
     *
     * @param now
     *            update date
     * @param rec
     *            DNS record
     */
    public void updateRecord(long now, DNSRecord rec)
    {
        // We do not want to block the entire DNS while we are updating the record for each listener (service info)
        {
            List<DNSListener> listenerList = null;
            synchronized (_listeners)
            {
                listenerList = new ArrayList<DNSListener>(_listeners);
            }
            for (DNSListener listener : listenerList)
            {
                listener.updateRecord(this.getCache(), now, rec);
            }
        }
        // FIXME [PJYF Mar 21 2010] This is bad we should not call listener while probing this result in incomplete info.
        if (DNSRecordType.TYPE_PTR.equals(rec.getRecordType()) || DNSRecordType.TYPE_SRV.equals(rec.getRecordType()))
        {
            List<ServiceListener> list = _serviceListeners.get(rec.getName().toLowerCase());
            List<ServiceListener> serviceListenerList = Collections.emptyList();
            if (list != null)
            {
                synchronized (list)
                {
                    serviceListenerList = new ArrayList<ServiceListener>(list);
                }
            }
            if (!serviceListenerList.isEmpty())
            {
                final boolean expired = rec.isExpired(now);
                final String type = rec.getName();
                final String name = (DNSRecordType.TYPE_PTR.equals(rec.getRecordType()) ? ((DNSRecord.Pointer) rec).getAlias() : ((DNSRecord.Service) rec).getServer());
                // DNSRecord old = (DNSRecord)services.get(name.toLowerCase());
                final ServiceEvent event = new ServiceEventImpl(this, type, toUnqualifiedName(type, name), rec.getServiceInfo());
                if (!expired)
                {
                    // new record
                    for (ServiceListener listener : serviceListenerList)
                    {
                        listener.serviceAdded(event);
                    }
                }
                else
                {
                    // expire record
                    for (ServiceListener listener : serviceListenerList)
                    {
                        listener.serviceRemoved(event);
                    }
                }
            }
        }
    }

    /**
     * Handle an incoming response. Cache answers, and pass them on to the appropriate questions.
     *
     * @throws IOException
     */
    void handleResponse(DNSIncoming msg) throws IOException
    {
        final long now = System.currentTimeMillis();

        boolean hostConflictDetected = false;
        boolean serviceConflictDetected = false;

        for (DNSRecord rec : msg.getAllAnswers())
        {
            boolean isInformative = false;
            final boolean expired = rec.isExpired(now);

            // update the cache
            final DNSRecord c = (DNSRecord) this.getCache().getDNSEntry(rec);
            if (c != null)
            {
                if (expired)
                {
                    isInformative = true;
                    this.getCache().removeDNSEntry(c);
                }
                else
                {
                    c.resetTTL(rec);
                    rec = c;
                }
            }
            else
            {
                if (!expired)
                {
                    isInformative = true;
                    this.getCache().addDNSEntry(rec);
                }
            }
            switch (rec.getRecordType())
            {
                case TYPE_PTR:
                    // handle _mdns._udp records
                    if (rec.getName().indexOf("._mdns._udp.") >= 0)
                    {
                        if (!expired && rec.getName().startsWith("_services._mdns._udp."))
                        {
                            isInformative = true;
                            this.registerServiceType(((DNSRecord.Pointer) rec)._alias);
                        }
                        continue;
                    }
                    registerServiceType(rec.getName());
                    break;
                default:
                    break;
            }

            if (DNSRecordType.TYPE_A.equals(rec.getRecordType()) || DNSRecordType.TYPE_AAAA.equals(rec.getRecordType()))
            {
                hostConflictDetected |= rec.handleResponse(this);
            }
            else
            {
                serviceConflictDetected |= rec.handleResponse(this);
            }

            // notify the listeners
            if (isInformative)
            {
                this.updateRecord(now, rec);
            }
        }

        if (hostConflictDetected || serviceConflictDetected)
        {
            new Prober(this).start(_timer);
        }
    }

    /**
     * Handle an incoming query. See if we can answer any part of it given our service infos.
     *
     * @param in
     * @param addr
     * @param port
     * @throws IOException
     */
    void handleQuery(DNSIncoming in, InetAddress addr, int port) throws IOException
    {
        // Track known answers
        boolean conflictDetected = false;
        final long expirationTime = System.currentTimeMillis() + DNSConstants.KNOWN_ANSWER_TTL;
        for (DNSRecord answer : in.getAllAnswers())
        {
            conflictDetected |= answer.handleQuery(this, expirationTime);
        }

        if (_plannedAnswer != null)
        {
            _plannedAnswer.append(in);
        }
        else
        {
            if (in.isTruncated())
            {
                _plannedAnswer = in;
            }
            new Responder(this, in, port).start();
        }

        if (conflictDetected)
        {
            new Prober(this).start(_timer);
        }
    }

    /**
     * Add an answer to a question. Deal with the case when the outgoing packet overflows
     *
     * @param in
     * @param addr
     * @param port
     * @param out
     * @param rec
     * @return outgoing answer
     * @throws IOException
     */
    public DNSOutgoing addAnswer(DNSIncoming in, InetAddress addr, int port, DNSOutgoing out, DNSRecord rec) throws IOException
    {
        DNSOutgoing newOut = out;
        if (newOut == null)
        {
            newOut = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA, false, in.getSenderUDPPayload());
        }
        try
        {
            newOut.addAnswer(in, rec);
        }
        catch (final IOException e)
        {
            newOut.setFlags(newOut.getFlags() | DNSConstants.FLAGS_TC);
            newOut.setId(in.getId());
            newOut.finish();
            send(newOut);

            newOut = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA, false, in.getSenderUDPPayload());
            newOut.addAnswer(in, rec);
        }
        return newOut;
    }

    /**
     * Send an outgoing multicast DNS message.
     *
     * @param out
     * @throws IOException
     */
    public void send(DNSOutgoing out) throws IOException
    {
        out.finish();
        if (!out.isEmpty())
        {
            final DatagramPacket packet = new DatagramPacket(out._data, out._off, _group, DNSConstants.MDNS_PORT);

            try
            {
                final DNSIncoming msg = new DNSIncoming(packet);
                logger.finest("send() JmDNS out:" + msg.print(true));
            }
            catch (final IOException e)
            {
                logger.throwing(getClass().toString(), "send(DNSOutgoing) - JmDNS can not parse what it sends!!!", e);
            }
            final MulticastSocket ms = _socket;
            if (ms != null && !ms.isClosed())
                ms.send(packet);
        }
    }

    public void startAnnouncer()
    {
        new Announcer(this).start(_timer);
    }

    public void startRenewer()
    {
        new Renewer(this).start(_timer);
    }

    public void schedule(TimerTask task, int delay)
    {
        if (this.getState() != DNSState.CANCELED)
        {
            _timer.schedule(task, delay);
        }
    }

    // REMIND: Why is this not an anonymous inner class?
    /**
     * Shutdown operations.
     */
    protected class Shutdown implements Runnable
    {
        public void run()
        {
            try
            {
                _shutdown = null;
                close();
            }
            catch (Throwable exception)
            {
                System.err.println("Error while shuting down. " + exception);
            }
        }
    }

    /**
     * Recover jmdns when there is an error.
     */
    public void recover()
    {
        logger.finer("recover()");
        // We have an IO error so lets try to recover if anything happens lets
        // close it.
        // This should cover the case of the IP address changing under our feet
        if (DNSState.CANCELED != getState())
        {
            synchronized (this)
            { // Synchronize only if we are not already in process to prevent
                // dead locks
                //
                logger.finer("recover() Cleanning up");
                // Stop JmDNS
                this.setState(DNSState.CANCELED); // This protects against recursive
                // calls

                // We need to keep a copy for reregistration
                final Collection<ServiceInfo> oldServiceInfos = new ArrayList<ServiceInfo>(getServices().values());

                // Cancel all services
                this.unregisterAllServices();
                this.disposeServiceCollectors();
                //
                // close multicast socket
                this.closeMulticastSocket();
                //
                this.getCache().clear();
                logger.finer("recover() All is clean");
                //
                // All is clear now start the services
                //
                try
                {
                    this.openMulticastSocket(this.getLocalHost());
                    this.start(oldServiceInfos);
                }
                catch (final Exception exception)
                {
                    logger.log(Level.WARNING, "recover() Start services exception ", exception);
                }
                logger.log(Level.WARNING, "recover() We are back!");
            }
        }
    }

    /**
     * @see javax.jmdns.JmDNS#close()
     */
    @Override
    public void close()
    {
        if (this.getState() != DNSState.CANCELED)
        {
            // Synchronize only if we are not already in process to prevent dead locks
            synchronized (this)
            {
                // Stop JmDNS
                // This protects against recursive calls
                this.setState(DNSState.CANCELED);

                // Stop the timer
                _timer.cancel();

                // Cancel all services
                this.unregisterAllServices();
                this.disposeServiceCollectors();

                // Stop the canceler timer
                _cancelerTimer.cancel();

                // close socket
                this.closeMulticastSocket();

                // remove the shutdown hook
                if (_shutdown != null)
                {
                    Runtime.getRuntime().removeShutdownHook(_shutdown);
                }

            }
        }
    }

    /**
     * List cache entries, for debugging only.
     */
    void print()
    {
        System.out.println(_cache.toString());
        System.out.println();
    }

    /**
     * @see javax.jmdns.JmDNS#printServices()
     */
    @Override
    public void printServices()
    {
        System.err.println(toString());
    }

    @Override
    public String toString()
    {
        final StringBuffer aLog = new StringBuffer();
        aLog.append("\t---- Services -----");
        for (String key : _services.keySet())
        {
            aLog.append("\n\t\tService: " + key + ": " + _services.get(key));
        }
        aLog.append("\n");
        aLog.append("\t---- Types ----");
        for (String key : _serviceTypes.keySet())
        {
            aLog.append("\n\t\tType: " + key + ": " + _serviceTypes.get(key));
        }
        aLog.append("\n");
        aLog.append(_cache.toString());
        aLog.append("\n");
        aLog.append("\t---- Service Collectors ----");
        for (String key : _serviceCollectors.keySet())
        {
            aLog.append("\n\t\tService Collector: " + key + ": " + _serviceCollectors.get(key));
        }
        return aLog.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#list(java.lang.String)
     */
    @Override
    public ServiceInfo[] list(String type)
    {
        return this.list(type, 6000);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.JmDNS#list(java.lang.String, int)
     */
    @Override
    public ServiceInfo[] list(String type, int timeout)
    {
        // Implementation note: The first time a list for a given type is
        // requested, a ServiceCollector is created which collects service
        // infos. This greatly speeds up the performance of subsequent calls
        // to this method. The caveats are, that 1) the first call to this
        // method
        // for a given type is slow, and 2) we spawn a ServiceCollector
        // instance for each service type which increases network traffic a
        // little.

        String aType = type.toLowerCase();

        ServiceCollector collector;

        boolean newCollectorCreated = false;
        //
        // 2009-09-16 ldeck: adding docbug patch with slight ammendments
        // 'Fixes two deadlock conditions involving JmDNS.close() - ID: 1473279'
        //
        synchronized (this) // to avoid possible deadlock with a close() in another thread
        {
            // If we've been cancelled but got the lock, we're about to die anyway,
            // so just return an empty array.
            if (_state == DNSState.CANCELED)
            {
                return new ServiceInfo[0];
            }

            collector = _serviceCollectors.get(aType);
            if (collector == null)
            {
                newCollectorCreated = _serviceCollectors.putIfAbsent(aType, new ServiceCollector(aType)) == null;
                collector = _serviceCollectors.get(aType);
                if (newCollectorCreated)
                {
                    this.addServiceListener(aType, collector);
                }
            }
        }

        return collector.list(timeout);
    }

    /**
     * This method disposes all ServiceCollector instances which have been created by calls to method <code>list(type)</code>.
     *
     * @see #list
     */
    private void disposeServiceCollectors()
    {
        logger.finer("disposeServiceCollectors()");
        for (String type : _serviceCollectors.keySet())
        {
            ServiceCollector collector = _serviceCollectors.get(type);
            if (collector != null)
            {
                this.removeServiceListener(type, collector);
            }
            _serviceCollectors.remove(type, collector);
        }
    }

    /**
     * Instances of ServiceCollector are used internally to speed up the performance of method <code>list(type)</code>.
     *
     * @see #list
     */
    private static class ServiceCollector implements ServiceListener
    {
        // private static Logger logger = Logger.getLogger(ServiceCollector.class.getName());

        /**
         * A set of collected service instance names.
         */
        private final ConcurrentMap<String, ServiceInfo> _infos;

        private final String _type;

        private final AtomicBoolean _shoudlWaitForList;

        public ServiceCollector(String type)
        {
            super();
            _infos = new ConcurrentHashMap<String, ServiceInfo>();
            _type = type;
            _shoudlWaitForList = new AtomicBoolean(true);
        }

        /**
         * A service has been added.
         *
         * @param event
         *            service event
         */
        public void serviceAdded(ServiceEvent event)
        {
            event.getDNS().requestServiceInfo(event.getType(), event.getName(), 0);
            _shoudlWaitForList.set(true);
        }

        /**
         * A service has been removed.
         *
         * @param event
         *            service event
         */
        public void serviceRemoved(ServiceEvent event)
        {
            _infos.remove(event.getName());
        }

        /**
         * A service has been resolved. Its details are now available in the ServiceInfo record.
         *
         * @param event
         *            service event
         */
        public void serviceResolved(ServiceEvent event)
        {
            _infos.put(event.getName(), event.getInfo());
        }

        /**
         * Returns an array of all service infos which have been collected by this ServiceCollector.
         *
         * @param timeout
         *            timeout if the info list is empty.
         *
         * @return Service Info array
         */
        public ServiceInfo[] list(long timeout)
        {
            if (_infos.isEmpty())
            {
                if (_shoudlWaitForList.get())
                {
                    long loops = (timeout / 200L);
                    if (loops < 1)
                    {
                        loops = 1;
                    }
                    for (int i = 0; i < loops; i++)
                    {
                        try
                        {
                            Thread.sleep(200);
                        }
                        catch (final InterruptedException e)
                        {
                            /* Stub */
                        }
                        if (!_infos.isEmpty())
                        {
                            break;
                        }
                    }
                    _shoudlWaitForList.set(false);
                }
            }
            return _infos.values().toArray(new ServiceInfo[_infos.size()]);
        }

        @Override
        public String toString()
        {
            final StringBuffer aLog = new StringBuffer();
            aLog.append("\n\tType: " + _type);
            if (_infos.isEmpty())
            {
                aLog.append("\n\tNo services collected.");
            }
            else
            {
                for (String key : _infos.keySet())
                {
                    aLog.append("\n\t\tService: " + key + ": " + _infos.get(key));
                }
            }
            return aLog.toString();
        }
    }

    static String toUnqualifiedName(String type, String qualifiedName)
    {
        if (qualifiedName.endsWith(type) && !(qualifiedName.equals(type)))
        {
            return qualifiedName.substring(0, qualifiedName.length() - type.length() - 1);
        }
        return qualifiedName;
    }

    public void setState(DNSState state)
    {
        this._state = state;
    }

    public void setTask(TimerTask task)
    {
        this._task = task;
    }

    public TimerTask getTask()
    {
        return _task;
    }

    public Map<String, ServiceInfo> getServices()
    {
        return _services;
    }

    public void setLastThrottleIncrement(long lastThrottleIncrement)
    {
        this._lastThrottleIncrement = lastThrottleIncrement;
    }

    public long getLastThrottleIncrement()
    {
        return _lastThrottleIncrement;
    }

    public void setThrottle(int throttle)
    {
        this._throttle = throttle;
    }

    public int getThrottle()
    {
        return _throttle;
    }

    public static Random getRandom()
    {
        return _random;
    }

    public void ioLock()
    {
        _ioLock.lock();
    }

    public void ioUnlock()
    {
        _ioLock.unlock();
    }

    public void setPlannedAnswer(DNSIncoming plannedAnswer)
    {
        this._plannedAnswer = plannedAnswer;
    }

    public DNSIncoming getPlannedAnswer()
    {
        return _plannedAnswer;
    }

    void setLocalHost(HostInfo localHost)
    {
        this._localHost = localHost;
    }

    public Map<String, String> getServiceTypes()
    {
        return _serviceTypes;
    }

    public void setClosed(boolean closed)
    {
        this._closed = closed;
    }

    public boolean isClosed()
    {
        return _closed;
    }

    public MulticastSocket getSocket()
    {
        return _socket;
    }

    public InetAddress getGroup()
    {
        return _group;
    }
}
