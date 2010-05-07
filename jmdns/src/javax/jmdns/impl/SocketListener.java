//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSState;

/**
 * Listen for multicast packets.
 */
class SocketListener implements Runnable
{
    static Logger logger = Logger.getLogger(SocketListener.class.getName());

    /**
     *
     */
    private final JmDNSImpl _jmDNSImpl;

    /**
     * @param jmDNSImpl
     */
    SocketListener(JmDNSImpl jmDNSImpl)
    {
        this._jmDNSImpl = jmDNSImpl;
    }

    public void run()
    {
        try
        {
            byte buf[] = new byte[DNSConstants.MAX_MSG_ABSOLUTE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (this._jmDNSImpl.getState() != DNSState.CANCELED)
            {
                packet.setLength(buf.length);
                this._jmDNSImpl.getSocket().receive(packet);
                if (this._jmDNSImpl.getState() == DNSState.CANCELED)
                {
                    break;
                }
                try
                {
                    if (this._jmDNSImpl.getLocalHost().shouldIgnorePacket(packet))
                    {
                        continue;
                    }

                    DNSIncoming msg = new DNSIncoming(packet);
                    if (logger.getLevel().intValue() <= Level.FINEST.intValue())
                    {
                        logger.finest("SocketListener.run() JmDNS in:" + msg.print(true));
                    }

                    this._jmDNSImpl.ioLock();
                    try
                    {
                        if (msg.isQuery())
                        {
                            if (packet.getPort() != DNSConstants.MDNS_PORT)
                            {
                                this._jmDNSImpl.handleQuery(msg, packet.getAddress(), packet.getPort());
                            }
                            this._jmDNSImpl.handleQuery(msg, this._jmDNSImpl.getGroup(), DNSConstants.MDNS_PORT);
                        }
                        else
                        {
                            this._jmDNSImpl.handleResponse(msg);
                        }
                    }
                    finally
                    {
                        this._jmDNSImpl.ioUnlock();
                    }
                }
                catch (IOException e)
                {
                    logger.log(Level.WARNING, "run() exception ", e);
                }
            }
        }
        catch (IOException e)
        {
            if (this._jmDNSImpl.getState() != DNSState.CANCELED)
            {
                logger.log(Level.WARNING, "run() exception ", e);
                this._jmDNSImpl.recover();
            }
        }
        // jP: 20010-01-18. Per issue #2933183. If this thread was stopped
        // by closeMulticastSocket, we need to signal the other party via
        // the jmDNS monitor. The other guy will then check to see if this
        // thread has died.
        // Note: This is placed here to avoid locking the IoLock object and
        // 'this' instance together.
        synchronized (this._jmDNSImpl)
        {
            this._jmDNSImpl.notifyAll();
        }
    }
}