// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.impl.constants.DNSConstants;

/**
 * Listen for multicast packets.
 */
class SocketListener extends Thread {
    static Logger           logger = LoggerFactory.getLogger(SocketListener.class.getName());

    /**
     *
     */
    private final JmDNSImpl _jmDNSImpl;

    /**
     * @param jmDNSImpl
     */
    SocketListener(JmDNSImpl jmDNSImpl) {
        super("SocketListener(" + (jmDNSImpl != null ? jmDNSImpl.getName() : "") + ")");
        this.setDaemon(true);
        this._jmDNSImpl = jmDNSImpl;
    }

    @Override
    public void run() {
        try {
            byte buf[] = new byte[DNSConstants.MAX_MSG_ABSOLUTE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            //while _jmDNSImpl not cancelled
            while (!this._jmDNSImpl.isCanceling() && !this._jmDNSImpl.isCanceled()) {

                //set packet length
                packet.setLength(buf.length);

                //receive from socket
                this._jmDNSImpl.getSocket().receive(packet);

                //check if _jmDNSImpl not cancelled
                if (this._jmDNSImpl.isCanceling() || this._jmDNSImpl.isCanceled() || this._jmDNSImpl.isClosing() || this._jmDNSImpl.isClosed()) {
                    break;
                }

                try {

                    //check if this packet is from any local interface then discard it.
                    if (this._jmDNSImpl.getLocalHost().shouldIgnorePacket(packet)) {
                        continue;
                    }

                    //convert datagram packet into DNSIncoming
                    DNSIncoming msg = new DNSIncoming(packet);

                    //is response code is valid == 0
                    if (msg.isValidResponseCode()) {

                        //log
                        if (logger.isTraceEnabled()) {
                            logger.trace(this.getName() + ".run() JmDNS in:" + msg.print(true));
                        }

                        if (msg.isQuery()) {

                            //this is a query message
                            if (packet.getPort() != DNSConstants.MDNS_PORT) {

                                //query
                                this._jmDNSImpl.handleQuery(msg, packet.getAddress(), packet.getPort());
                            }

                            //query
                            this._jmDNSImpl.handleQuery(msg, this._jmDNSImpl.getGroup(), DNSConstants.MDNS_PORT);

                        } else {

                            //this is a response message to previous query so take care of response
                            this._jmDNSImpl.handleResponse(msg);
                        }
                    } else {
                        //log
                        if (logger.isDebugEnabled()) {
                            logger.debug(this.getName() + ".run() JmDNS in message with error code:" + msg.print(true));
                        }
                    }
                } catch (IOException e) {
                    logger.warn(this.getName() + ".run() exception ", e);
                }
            }
        } catch (IOException e) {
            if (!this._jmDNSImpl.isCanceling() && !this._jmDNSImpl.isCanceled() && !this._jmDNSImpl.isClosing() && !this._jmDNSImpl.isClosed()) {
                logger.warn(this.getName() + ".run() exception ", e);
                this._jmDNSImpl.recover();
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace(this.getName() + ".run() exiting.");
        }
    }

    public JmDNSImpl getDns() {
        return _jmDNSImpl;
    }

}
