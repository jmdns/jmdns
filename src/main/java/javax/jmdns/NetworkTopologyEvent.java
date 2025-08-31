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
package javax.jmdns;

import java.net.InetAddress;
import java.util.EventObject;

/**
 * Represents an event that occurs when the network topology changes.
 * 
 * @author C&eacute;drik Lime, Pierre Frisch
 */
public abstract class NetworkTopologyEvent extends EventObject {

    /**
     *
     */
    private static final long serialVersionUID = -8630033521752540987L;

    /**
     * Constructs a Service Event.
     * 
     * @param eventSource
     *            The DNS on which the Event initially occurred.
     * @exception IllegalArgumentException
     *                if source is null.
     */
    protected NetworkTopologyEvent(final Object eventSource) {
        super(eventSource);
    }

    /**
     * Returns the JmDNS instance associated with the event or null if it is a generic event.
     * 
     * @return JmDNS instance
     */
    public abstract JmDNS getDNS();

    /**
     * The Internet address affected by this event.
     * 
     * @return InetAddress
     */
    public abstract InetAddress getInetAddress();

}
