//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * DNSState defines the possible states for services registered with JmDNS.
 *
 * @author Werner Randelshofer, Rick Blair
 * @version 1.0 May 23, 2004 Created.
 */
public class DNSState implements Comparable<DNSState>
{
    // private static Logger logger = Logger.getLogger(DNSState.class.getName());

    private final String _name;

    /**
     * Ordinal of next state to be created.
     */
    private static int nextOrdinal = 0;
    /**
     * Assign an ordinal to this state.
     */
    private final int ordinal = nextOrdinal++;
    /**
     * Logical sequence of states. The sequence is consistent with the ordinal of a state. This is used for advancing
     * through states.
     */
    private final static List<DNSState> sequence = new ArrayList<DNSState>();

    private DNSState(String name)
    {
        this._name = name;
        sequence.add(this);
    }

    @Override
    public final String toString()
    {
        return _name;
    }

    public static final DNSState PROBING_1 = new DNSState("probing 1");
    public static final DNSState PROBING_2 = new DNSState("probing 2");
    public static final DNSState PROBING_3 = new DNSState("probing 3");
    public static final DNSState ANNOUNCING_1 = new DNSState("announcing 1");
    public static final DNSState ANNOUNCING_2 = new DNSState("announcing 2");
    public static final DNSState ANNOUNCED = new DNSState("announced");
    public static final DNSState CANCELED = new DNSState("canceled");

    /**
     * Returns the next advanced state. In general, this advances one step in the following sequence: PROBING_1,
     * PROBING_2, PROBING_3, ANNOUNCING_1, ANNOUNCING_2, ANNOUNCED. Does not advance for ANNOUNCED and CANCELED state.
     *
     * @return next state
     */
    public final DNSState advance()
    {
        return (isProbing() || isAnnouncing()) ? (DNSState) sequence.get(ordinal + 1) : this;
    }

    /**
     * Returns to the next reverted state. All states except CANCELED revert to PROBING_1. Status CANCELED does not
     * revert.
     *
     * @return reverted state
     */
    public final DNSState revert()
    {
        return (this == CANCELED) ? this : PROBING_1;
    }

    /**
     * Returns true, if this is a probing state.
     *
     * @return <code>true</code> if probing state, <code>false</code> otherwise
     */
    public boolean isProbing()
    {
        return compareTo(PROBING_1) >= 0 && compareTo(PROBING_3) <= 0;
    }

    /**
     * Returns true, if this is an announcing state.
     *
     * @return <code>true</code> if announcing state, <code>false</code> otherwise
     */
    public boolean isAnnouncing()
    {
        return compareTo(ANNOUNCING_1) >= 0 && compareTo(ANNOUNCING_2) <= 0;
    }

    /**
     * Returns true, if this is an announced state.
     *
     * @return <code>true</code> if announced state, <code>false</code> otherwise
     */
    public boolean isAnnounced()
    {
        return compareTo(ANNOUNCED) == 0;
    }

    /**
     * Compares two states. The states compare as follows: PROBING_1 &lt; PROBING_2 &lt; PROBING_3 &lt; ANNOUNCING_1
     * &lt; ANNOUNCING_2 &lt; RESPONDING &lt; ANNOUNCED &lt; CANCELED.
     *
     * @param o
     *            the DNSState to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     */
    public int compareTo(DNSState o)
    {
        return ordinal - o.ordinal;
    }
}