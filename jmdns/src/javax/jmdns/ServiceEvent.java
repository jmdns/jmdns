// %Z%%M%, %I%, %G%
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package javax.jmdns;

import java.util.EventObject;
/**
 * ServiceEvent.
 *
 * @author  Werner Randelshofer
 * @version 	%I%, %G%
 */
public class ServiceEvent extends EventObject {
    /**
     * The type name of the service.
     */
    private String type;
    /**
     * The instance name of the service. Or null, if the event was
     * fired to a service type listener.
     */
    private String name;
    /**
     * The service info record, or null if the service could be be resolved.
     * This is also null, if the event was fired to a service type listener.
     */
    private ServiceInfo info;
    
    /**
     * Creates a new instance.
     *
     * @param source the JmDNS instance which originated the event.
     * @param type the type name of the service.
     * @param name the instance name of the service.
     * @param info the service info record, or null if the service could be be resolved.
     */
    public ServiceEvent(JmDNS source, String type, String name, ServiceInfo info) {
        super(source);
        this.type = type;
        this.name = name;
        this.info = info;
    }
    
    /**
     * Returns the JmDNS instance which originated the event.
     */
    public JmDNS getDNS() {
        return (JmDNS) getSource();
    }
    
    /**
     * Returns the fully qualified type of the service.
     */
    public String getType() {
        return type;
    }
    
    /**
     * Returns the instance name of the service.
     * Always returns null, if the event is sent to a service type listener.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the service info record, or null if the service could not be
     * resolved.
     * Always returns null, if the event is sent to a service type listener.
     */
    public ServiceInfo getInfo() {
        return info;
    }
}
