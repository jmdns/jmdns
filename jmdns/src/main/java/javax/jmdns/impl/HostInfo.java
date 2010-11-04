//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;
import javax.jmdns.impl.tasks.DNSTask;

/**
 * HostInfo information on the local host to be able to cope with change of addresses.
 *
 * @version %I%, %G%
 * @author Pierre Frisch, Werner Randelshofer
 */
public class HostInfo implements DNSStatefulObject
{
    private static Logger logger = Logger.getLogger(HostInfo.class.getName());

    protected String _name;

    protected InetAddress _address;

    protected NetworkInterface _interfaze;

    private final HostInfoState _state;

    private final static class HostInfoState extends DNSStatefulObject.DefaultImplementation
    {

        private static final long serialVersionUID = -8191476803620402088L;

        /**
         * @param dns
         */
        public HostInfoState(JmDNSImpl dns)
        {
            super();
            this.setDns(dns);
        }

    }

    public static HostInfo newHostInfo(InetAddress address, JmDNSImpl dns)
    {
        HostInfo localhost = null;
        try
        {
            InetAddress addr = address;
            String aName = "";
            if (addr == null)
            {
                String ip = System.getProperty("net.mdns.interface");
                if (ip != null)
                {
                    addr = InetAddress.getByName(ip);
                }
                else
                {
                    addr = InetAddress.getLocalHost();
                    if (addr.isLoopbackAddress())
                    {
                        // Find local address that isn't a loopback address
                        for (Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces(); nifs.hasMoreElements() && addr.isLoopbackAddress();)
                        {
                            NetworkInterface nif = nifs.nextElement();
                            for (Enumeration<InetAddress> iaenum = nif.getInetAddresses(); iaenum.hasMoreElements();)
                            {
                                InetAddress interfaceAddress = iaenum.nextElement();
                                if (useInetAddress(nif, interfaceAddress))
                                {
                                    addr = interfaceAddress;
                                    break;
                                }
                            }
                        }
                    }
                }
                aName = addr.getHostName();
                if (addr.isLoopbackAddress())
                {
                    logger.warning("Could not find any address beside the loopback.");
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
            localhost = new HostInfo(addr, aName, dns);
        }
        catch (final IOException e)
        {
            logger.log(Level.WARNING, "Could not intialize the host network interface on " + address + "because of an error: " + e.getMessage(), e);
            // This is only used for running unit test on Debian / Ubuntu
            localhost = new HostInfo(loopbackAddress(), "computer", dns);
        }
        return localhost;
    }

    private static InetAddress loopbackAddress()
    {
        try
        {
            return InetAddress.getByName(null);
        }
        catch (UnknownHostException exception)
        {
            return null;
        }
    }

    private static boolean useInetAddress(NetworkInterface networkInterface, InetAddress interfaceAddress)
    {
        try
        {
            if (!networkInterface.isUp())
            {
                return false;
            }
            if (!networkInterface.supportsMulticast())
            {
                return false;
            }
            if (interfaceAddress.isLoopbackAddress())
            {
                return false;
            }
            return true;
        }
        catch (Exception exception)
        {
            return false;
        }
    }

    /**
     * This is used to create a unique name for the host name.
     */
    private int hostNameCount;

    public HostInfo(InetAddress address, String name, JmDNSImpl dns)
    {
        super();
        this._state = new HostInfoState(dns);
        this._address = address;
        this._name = name;
        if (address != null)
        {
            try
            {
                _interfaze = NetworkInterface.getByInetAddress(address);
            }
            catch (Exception exception)
            {
                // FIXME Shouldn't we take an action here?
                logger.log(Level.WARNING, "LocalHostInfo() exception ", exception);
            }
        }
    }

    public String getName()
    {
        return _name;
    }

    public InetAddress getInetAddress()
    {
        return _address;
    }

    Inet4Address getInet4Address()
    {
        if (this.getInetAddress() instanceof Inet4Address)
        {
            return (Inet4Address) _address;
        }
        return null;
    }

    Inet6Address getInet6Address()
    {
        if (this.getInetAddress() instanceof Inet6Address)
        {
            return (Inet6Address) _address;
        }
        return null;
    }

    public NetworkInterface getInterface()
    {
        return _interfaze;
    }

    synchronized String incrementHostName()
    {
        hostNameCount++;
        int plocal = _name.indexOf(".local.");
        int punder = _name.lastIndexOf("-");
        _name = _name.substring(0, (punder == -1 ? plocal : punder)) + "-" + hostNameCount + ".local.";
        return _name;
    }

    boolean shouldIgnorePacket(DatagramPacket packet)
    {
        boolean result = false;
        if (this.getInetAddress() != null)
        {
            InetAddress from = packet.getAddress();
            if (from != null)
            {
                if (from.isLinkLocalAddress() && (!this.getInetAddress().isLinkLocalAddress()))
                {
                    // Ignore linklocal packets on regular interfaces, unless this is
                    // also a linklocal interface. This is to avoid duplicates. This is
                    // a terrible hack caused by the lack of an API to get the address
                    // of the interface on which the packet was received.
                    result = true;
                }
                if (from.isLoopbackAddress() && (!this.getInetAddress().isLoopbackAddress()))
                {
                    // Ignore loopback packets on a regular interface unless this is also a loopback interface.
                    result = true;
                }
            }
        }
        return result;
    }

    DNSRecord.Address getDNSAddressRecord(DNSRecordType type, boolean unique, int ttl)
    {
        switch (type)
        {
            case TYPE_A:
                return this.getDNS4AddressRecord(unique, ttl);
            case TYPE_AAAA:
                return this.getDNS6AddressRecord(unique, ttl);
            default:
        }
        return null;
    }

    private DNSRecord.Address getDNS4AddressRecord(boolean unique, int ttl)
    {
        if ((this.getInetAddress() instanceof Inet4Address) || ((this.getInetAddress() instanceof Inet6Address) && (((Inet6Address) this.getInetAddress()).isIPv4CompatibleAddress())))
        {
            return new DNSRecord.IPv4Address(this.getName(), DNSRecordClass.CLASS_IN, unique, ttl, this.getInetAddress());
        }
        return null;
    }

    private DNSRecord.Address getDNS6AddressRecord(boolean unique, int ttl)
    {
        if (this.getInetAddress() instanceof Inet6Address)
        {
            return new DNSRecord.IPv6Address(this.getName(), DNSRecordClass.CLASS_IN, unique, ttl, this.getInetAddress());
        }
        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(1024);
        buf.append("local host info[");
        buf.append(getName() != null ? getName() : "no name");
        buf.append(", ");
        buf.append(getInterface() != null ? getInterface().getDisplayName() : "???");
        buf.append(":");
        buf.append(getInetAddress() != null ? getInetAddress().getHostAddress() : "no address");
        buf.append(", ");
        buf.append(_state);
        buf.append("]");
        return buf.toString();
    }

    public Collection<DNSRecord> answers(boolean unique, int ttl)
    {
        List<DNSRecord> list = new ArrayList<DNSRecord>();
        DNSRecord answer = this.getDNS4AddressRecord(unique, ttl);
        if (answer != null)
        {
            list.add(answer);
        }
        answer = this.getDNS6AddressRecord(unique, ttl);
        if (answer != null)
        {
            list.add(answer);
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JmDNSImpl getDns()
    {
        return this._state.getDns();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean advanceState(DNSTask task)
    {
        return this._state.advanceState(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAssociationWithTask(DNSTask task)
    {
        this._state.removeAssociationWithTask(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean revertState()
    {
        return this._state.revertState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void associateWithTask(DNSTask task, DNSState state)
    {
        this._state.associateWithTask(task, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAssociatedWithTask(DNSTask task, DNSState state)
    {
        return this._state.isAssociatedWithTask(task, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancelState()
    {
        return this._state.cancelState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean recoverState()
    {
        return this._state.recoverState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isProbing()
    {
        return this._state.isProbing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnnouncing()
    {
        return this._state.isAnnouncing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnnounced()
    {
        return this._state.isAnnounced();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCanceling()
    {
        return this._state.isCanceling();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCanceled()
    {
        return this._state.isCanceled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean waitForAnnounced(long timeout)
    {
        return _state.waitForAnnounced(timeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean waitForCanceled(long timeout)
    {
        if (_address == null)
        {
            // No need to wait this was never announced.
            return true;
        }
        return _state.waitForCanceled(timeout);
    }

}
