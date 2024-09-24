// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl.constants;

/**
 * DNSState defines the possible states for services registered with JmDNS.
 *
 * @author Werner Randelshofer, Rick Blair, Pierre Frisch
 */
public enum DNSState {

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
    CANCELING_1("canceling 1", StateClass.canceling),
    /**
    *
    */
    CANCELING_2("canceling 2", StateClass.canceling),
    /**
    *
    */
    CANCELING_3("canceling 3", StateClass.canceling),
    /**
    *
    */
    CANCELED("canceled", StateClass.canceled),
    /**
     *
     */
    CLOSING("closing", StateClass.closing),
    /**
     *
     */
    CLOSED("closed", StateClass.closed);

    private enum StateClass {
        probing, announcing, announced, canceling, canceled, closing, closed
    }

    // private static Logger logger = LoggerFactory.getLogger(DNSState.class);

    private final String     _name;

    private final StateClass _state;

    DNSState(String name, StateClass state) {
        _name = name;
        _state = state;
    }

    @Override
    public final String toString() {
        return _name;
    }

    /**
     * Returns the next advanced state.<br/>
     * In general, this advances one step in the following sequence: PROBING_1, PROBING_2, PROBING_3, ANNOUNCING_1, ANNOUNCING_2, ANNOUNCED.<br/>
     * or CANCELING_1, CANCELING_2, CANCELING_3, CANCELED Does not advance for ANNOUNCED and CANCELED state.
     *
     * @return next state
     */
    public final DNSState advance() {
        return switch (this) {
            case PROBING_1 -> PROBING_2;
            case PROBING_2 -> PROBING_3;
            case PROBING_3 -> ANNOUNCING_1;
            case ANNOUNCING_1 -> ANNOUNCING_2;
            case ANNOUNCING_2, ANNOUNCED -> ANNOUNCED;
            case CANCELING_1 -> CANCELING_2;
            case CANCELING_2 -> CANCELING_3;
            case CANCELING_3, CANCELED -> CANCELED;
            case CLOSING, CLOSED -> CLOSED;
        };
    }

    /**
     * Returns to the next reverted state. All states except CANCELED revert to PROBING_1. Status CANCELED does not revert.
     *
     * @return reverted state
     */
    public final DNSState revert() {
        return switch (this) {
            case PROBING_1, PROBING_2, PROBING_3, ANNOUNCING_1, ANNOUNCING_2, ANNOUNCED -> PROBING_1;
            case CANCELING_1, CANCELING_2, CANCELING_3 -> CANCELING_1;
            case CANCELED -> CANCELED;
            case CLOSING -> CLOSING;
            case CLOSED -> CLOSED;
        };
    }

    /**
     * Returns true, if this is a probing state.
     *
     * @return <code>true</code> if probing state, <code>false</code> otherwise
     */
    public final boolean isProbing() {
        return _state == StateClass.probing;
    }

    /**
     * Returns true, if this is an announcing state.
     *
     * @return <code>true</code> if announcing state, <code>false</code> otherwise
     */
    public final boolean isAnnouncing() {
        return _state == StateClass.announcing;
    }

    /**
     * Returns true, if this is an announced state.
     *
     * @return <code>true</code> if announced state, <code>false</code> otherwise
     */
    public final boolean isAnnounced() {
        return _state == StateClass.announced;
    }

    /**
     * Returns true, if this is a canceling state.
     *
     * @return <code>true</code> if canceling state, <code>false</code> otherwise
     */
    public final boolean isCanceling() {
        return _state == StateClass.canceling;
    }

    /**
     * Returns true, if this is a canceled state.
     *
     * @return <code>true</code> if canceled state, <code>false</code> otherwise
     */
    public final boolean isCanceled() {
        return _state == StateClass.canceled;
    }

    /**
     * Returns true, if this is a closing state.
     *
     * @return <code>true</code> if closing state, <code>false</code> otherwise
     */
    public final boolean isClosing() {
        return _state == StateClass.closing;
    }

    /**
     * Returns true, if this is a closing state.
     *
     * @return <code>true</code> if closed state, <code>false</code> otherwise
     */
    public final boolean isClosed() {
        return _state == StateClass.closed;
    }

}