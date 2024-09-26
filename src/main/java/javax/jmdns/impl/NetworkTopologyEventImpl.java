/**
 *
 */
package javax.jmdns.impl;

import java.io.Serial;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.NetworkTopologyEvent;
import javax.jmdns.NetworkTopologyListener;

/**
 * @author C&eacute;drik Lime, Pierre Frisch
 */
public class NetworkTopologyEventImpl extends NetworkTopologyEvent implements Cloneable {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 1445606146153550463L;

    private final InetAddress _inetAddress;

    /**
     * Constructs a Network Topology Event.
     * 
     * @param jmDNS
     * @param inetAddress
     * @exception IllegalArgumentException
     *                if source is null.
     */
    public NetworkTopologyEventImpl(JmDNS jmDNS, InetAddress inetAddress) {
        super(jmDNS);
        this._inetAddress = inetAddress;
    }

    NetworkTopologyEventImpl(NetworkTopologyListener jmmDNS, InetAddress inetAddress) {
        super(jmmDNS);
        this._inetAddress = inetAddress;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.NetworkTopologyEvent#getDNS()
     */
    @Override
    public JmDNS getDNS() {
        return (this.getSource() instanceof JmDNS ? (JmDNS) getSource() : null);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.NetworkTopologyEvent#getInetAddress()
     */
    @Override
    public InetAddress getInetAddress() {
        return _inetAddress;
    }

    @Override
    public String toString() {
        //            .append("' source: ")
//            .append("\n\t" + source + "")
//            .append("\n]");
        return '[' + this.getClass().getSimpleName() + '@' + System.identityHashCode(this) +
                "\n\tinetAddress: '" +
                this.getInetAddress() +
                "']";
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public NetworkTopologyEventImpl clone() throws CloneNotSupportedException {
        return new NetworkTopologyEventImpl(getDNS(), getInetAddress());
    }

}