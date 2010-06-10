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

        /**
         * @param dns
         */
        public HostInfoState(JmDNSImpl dns)
        {
            super();
            this.setDns(dns);
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

    public InetAddress getAddress()
    {
        return _address;
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
        if (getAddress() != null)
        {
            InetAddress from = packet.getAddress();
            if (from != null)
            {
                if (from.isLinkLocalAddress() && (!getAddress().isLinkLocalAddress()))
                {
                    // Ignore linklocal packets on regular interfaces, unless this is
                    // also a linklocal interface. This is to avoid duplicates. This is
                    // a terrible hack caused by the lack of an API to get the address
                    // of the interface on which the packet was received.
                    result = true;
                }
                if (from.isLoopbackAddress() && (!getAddress().isLoopbackAddress()))
                {
                    // Ignore loopback packets on a regular interface unless this is
                    // also a loopback interface.
                    result = true;
                }
            }
        }
        return result;
    }

    DNSRecord.Address getDNSAddressRecord(DNSRecord.Address address, int ttl)
    {
        return (DNSRecordType.TYPE_AAAA.equals(address.getRecordType()) ? this.getDNS6AddressRecord(ttl) : this.getDNS4AddressRecord(ttl));
    }

    public DNSRecord.Address getDNS4AddressRecord(int ttl)
    {
        if ((this.getAddress() instanceof Inet4Address) || ((this.getAddress() instanceof Inet6Address) && (((Inet6Address) this.getAddress()).isIPv4CompatibleAddress())))
        {
            return new DNSRecord.Address(this.getName(), DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, ttl, this.getAddress());
        }
        return null;
    }

    public DNSRecord.Address getDNS6AddressRecord(int ttl)
    {
        if (this.getAddress() instanceof Inet6Address)
        {
            return new DNSRecord.Address(this.getName(), DNSRecordType.TYPE_AAAA, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, ttl, this.getAddress());
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
        buf.append(getAddress() != null ? getAddress().getHostAddress() : "no address");
        buf.append(", ");
        buf.append(_state);
        buf.append("]");
        return buf.toString();
    }

    public void addAddressRecords(DNSOutgoing out, int ttl, boolean authoritative) throws IOException
    {
        DNSRecord answer = this.getDNS4AddressRecord(ttl);
        if (answer != null)
        {
            if (authoritative)
            {
                out.addAuthorativeAnswer(answer);
            }
            else
            {
                out.addAnswer(answer, 0);
            }
        }

        answer = this.getDNS6AddressRecord(ttl);
        if (answer != null)
        {
            if (authoritative)
            {
                out.addAuthorativeAnswer(answer);
            }
            else
            {
                out.addAnswer(answer, 0);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#getDns()
     */
    @Override
    public JmDNSImpl getDns()
    {
        return this._state.getDns();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#advanceState()
     */
    @Override
    public boolean advanceState()
    {
        return this._state.advanceState();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#removeAssociationWithTask(javax.jmdns.impl.tasks.DNSTask)
     */
    @Override
    public void removeAssociationWithTask(DNSTask task)
    {
        this._state.removeAssociationWithTask(task);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#revertState()
     */
    @Override
    public boolean revertState()
    {
        return this._state.revertState();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#associateWithTask(javax.jmdns.impl.tasks.DNSTask, javax.jmdns.impl.constants.DNSState)
     */
    @Override
    public void associateWithTask(DNSTask task, DNSState state)
    {
        this._state.associateWithTask(task, state);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#isAssociatedWithTask(javax.jmdns.impl.tasks.DNSTask, javax.jmdns.impl.constants.DNSState)
     */
    @Override
    public boolean isAssociatedWithTask(DNSTask task, DNSState state)
    {
        return this._state.isAssociatedWithTask(task, state);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#cancel()
     */
    @Override
    public boolean cancelState()
    {
        return this._state.cancelState();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#recover()
     */
    @Override
    public boolean recoverState()
    {
        return this._state.recoverState();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#isProbing()
     */
    @Override
    public boolean isProbing()
    {
        return this._state.isProbing();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#isAnnouncing()
     */
    @Override
    public boolean isAnnouncing()
    {
        return this._state.isAnnouncing();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#isAnnounced()
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
     * @see javax.jmdns.impl.DNSStatefulObject#isCanceled()
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
        return _state.waitForAnnounced(timeout);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#waitForCanceled(long)
     */
    @Override
    public boolean waitForCanceled(long timeout)
    {
        return _state.waitForCanceled(timeout);
    }

}
