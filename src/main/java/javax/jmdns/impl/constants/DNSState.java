/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.jmdns.impl.constants;

/**
 * DNSState defines the possible states for services registered with JmDNS.
 *
 * @author Werner Randelshofer, Rick Blair, Pierre Frisch
 */
public enum DNSState {
    PROBING_1("probing 1", StateClass.probing),
    PROBING_2("probing 2", StateClass.probing),
    PROBING_3("probing 3", StateClass.probing),
    ANNOUNCING_1("announcing 1", StateClass.announcing),
    ANNOUNCING_2("announcing 2", StateClass.announcing),
    ANNOUNCED("announced", StateClass.announced),
    CANCELING_1("canceling 1", StateClass.canceling),
    CANCELING_2("canceling 2", StateClass.canceling),
    CANCELING_3("canceling 3", StateClass.canceling),
    CANCELED("canceled", StateClass.canceled),
    CLOSING("closing", StateClass.closing),
    CLOSED("closed", StateClass.closed);

    private enum StateClass {
        probing, announcing, announced, canceling, canceled, closing, closed
    }

    private final String name;

    private final StateClass state;

    DNSState(String name, StateClass state) {
        this.name = name;
        this.state = state;
    }

    @Override
    public final String toString() {
        return name;
    }

    /**
     * Returns the next advanced state.<br/>
     * In general, this advances one step in the following sequence: PROBING_1, PROBING_2, PROBING_3, ANNOUNCING_1, ANNOUNCING_2, ANNOUNCED.<br/>
     * or CANCELING_1, CANCELING_2, CANCELING_3, CANCELED Does not advance for ANNOUNCED and CANCELED state.
     *
     * @return next state
     */
    public final DNSState advance() {
        switch (this) {
            case PROBING_1:
                return PROBING_2;
            case PROBING_2:
                return PROBING_3;
            case PROBING_3:
                return ANNOUNCING_1;
            case ANNOUNCING_1:
                return ANNOUNCING_2;
            case ANNOUNCING_2:
            case ANNOUNCED:
                return ANNOUNCED;
            case CANCELING_1:
                return CANCELING_2;
            case CANCELING_2:
                return CANCELING_3;
            case CANCELING_3:
            case CANCELED:
                return CANCELED;
            case CLOSING:
            case CLOSED:
                return CLOSED;
            default:
                // This is just to keep the compiler happy as we have covered all cases before.
                return this;
        }
    }

    /**
     * Returns to the next reverted state. All states except CANCELED revert to PROBING_1. Status CANCELED does not revert.
     *
     * @return reverted state
     */
    public final DNSState revert() {
        switch (this) {
            case PROBING_1:
            case PROBING_2:
            case PROBING_3:
            case ANNOUNCING_1:
            case ANNOUNCING_2:
            case ANNOUNCED:
                return PROBING_1;
            case CANCELING_1:
            case CANCELING_2:
            case CANCELING_3:
                return CANCELING_1;
            case CANCELED:
                return CANCELED;
            case CLOSING:
                return CLOSING;
            case CLOSED:
                return CLOSED;
            default:
                // This is just to keep the compiler happy as we have covered all cases before.
                return this;
        }
    }

    /**
     * Returns true, if this is a probing state.
     *
     * @return <code>true</code> if probing state, <code>false</code> otherwise
     */
    public final boolean isProbing() {
        return state == StateClass.probing;
    }

    /**
     * Returns true, if this is an announcing state.
     *
     * @return <code>true</code> if announcing state, <code>false</code> otherwise
     */
    public final boolean isAnnouncing() {
        return state == StateClass.announcing;
    }

    /**
     * Returns true, if this is an announced state.
     *
     * @return <code>true</code> if announced state, <code>false</code> otherwise
     */
    public final boolean isAnnounced() {
        return state == StateClass.announced;
    }

    /**
     * Returns true, if this is a canceling state.
     *
     * @return <code>true</code> if canceling state, <code>false</code> otherwise
     */
    public final boolean isCanceling() {
        return state == StateClass.canceling;
    }

    /**
     * Returns true, if this is a canceled state.
     *
     * @return <code>true</code> if canceled state, <code>false</code> otherwise
     */
    public final boolean isCanceled() {
        return state == StateClass.canceled;
    }

    /**
     * Returns true, if this is a closing state.
     *
     * @return <code>true</code> if closing state, <code>false</code> otherwise
     */
    public final boolean isClosing() {
        return state == StateClass.closing;
    }

    /**
     * Returns true, if this is a closing state.
     *
     * @return <code>true</code> if closed state, <code>false</code> otherwise
     */
    public final boolean isClosed() {
        return state == StateClass.closed;
    }

}