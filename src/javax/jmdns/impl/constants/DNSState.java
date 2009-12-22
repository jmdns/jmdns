//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.constants;

/**
 * DNSState defines the possible states for services registered with JmDNS.
 *
 * @author Werner Randelshofer, Rick Blair
 * @version 1.0 May 23, 2004 Created.
 */
public enum DNSState
{

    /**
     *
     */
    PROBING_1("probing 1", StateClass.probing),
    /**
    *
    */
    PROBING_2("probing 2", StateClass.probing),
    /**
    *
    */
    PROBING_3("probing 3", StateClass.probing),
    /**
    *
    */
    ANNOUNCING_1("announcing 1", StateClass.announcing),
    /**
    *
    */
    ANNOUNCING_2("announcing 2", StateClass.announcing),
    /**
    *
    */
    ANNOUNCED("announced", StateClass.announced),
    /**
    *
    */
    CANCELED("canceled", StateClass.canceled);

    private enum StateClass
    {
        probing, announcing, announced, canceled
    }

    // private static Logger logger = Logger.getLogger(DNSState.class.getName());

    private final String _name;

    private final StateClass _state;

    private DNSState(String name, StateClass state)
    {
        _name = name;
        _state = state;
    }

    @Override
    public final String toString()
    {
        return _name;
    }

    /**
     * Returns the next advanced state. In general, this advances one step in the following sequence: PROBING_1,
     * PROBING_2, PROBING_3, ANNOUNCING_1, ANNOUNCING_2, ANNOUNCED. Does not advance for ANNOUNCED and CANCELED state.
     *
     * @return next state
     */
    public final DNSState advance()
    {
        switch (this)
        {
            case PROBING_1:
                return PROBING_2;
            case PROBING_2:
                return PROBING_3;
            case PROBING_3:
                return ANNOUNCING_1;
            case ANNOUNCING_1:
                return ANNOUNCING_2;
            case ANNOUNCING_2:
                return ANNOUNCED;
            case ANNOUNCED:
                return ANNOUNCED;
            case CANCELED:
                return CANCELED;
        }
        // This is just to keep the compiler happy as we have covered all cases before.
        return this;
    }

    /**
     * Returns to the next reverted state. All states except CANCELED revert to PROBING_1. Status CANCELED does not
     * revert.
     *
     * @return reverted state
     */
    public final DNSState revert()
    {
        return _state == StateClass.canceled ? CANCELED : PROBING_1;
    }

    /**
     * Returns true, if this is a probing state.
     *
     * @return <code>true</code> if probing state, <code>false</code> otherwise
     */
    public final boolean isProbing()
    {
        return _state == StateClass.probing;
    }

    /**
     * Returns true, if this is an announcing state.
     *
     * @return <code>true</code> if announcing state, <code>false</code> otherwise
     */
    public final boolean isAnnouncing()
    {
        return _state == StateClass.announcing;
    }

    /**
     * Returns true, if this is an announced state.
     *
     * @return <code>true</code> if announced state, <code>false</code> otherwise
     */
    public final boolean isAnnounced()
    {
        return _state == StateClass.announced;
    }

    /**
     * Returns true, if this is a canceled state.
     *
     * @return <code>true</code> if canceled state, <code>false</code> otherwise
     */
    public final boolean isCanceled()
    {
        return _state == StateClass.canceled;
    }

}