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

import java.util.EventObject;

public abstract class ServiceEvent extends EventObject implements Cloneable {

    /**
     *
     */
    private static final long serialVersionUID = -8558445644541006271L;

    /**
     * Constructs a Service Event.
     * 
     * @param eventSource
     *            The object on which the Event initially occurred.
     * @exception IllegalArgumentException
     *                if source is null.
     */
    public ServiceEvent(final Object eventSource) {
        super(eventSource);
    }

    /**
     * Returns the JmDNS instance which originated the event.
     * 
     * @return JmDNS instance
     */
    public abstract JmDNS getDNS();

    /**
     * Returns the fully qualified type of the service.
     * 
     * @return type of the service.
     */
    public abstract String getType();

    /**
     * Returns the instance name of the service. Always returns null, if the event is sent to a service type listener.
     * 
     * @return name of the service
     */
    public abstract String getName();

    /**
     * Returns the service info record, or null if the service could not be resolved. Always returns null, if the event is sent to a service type listener.
     * 
     * @return service info record
     * @see javax.jmdns.ServiceEvent#getInfo()
     */
    public abstract ServiceInfo getInfo();

    /*
     * (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public ServiceEvent clone() {
        try {
            return (ServiceEvent) super.clone();
        } catch (CloneNotSupportedException exception) {
            // clone is supported
            return null;
        }
    }

}