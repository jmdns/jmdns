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

import java.util.EventListener;

/**
 * Listener for service updates.
 *
 * @version %I%, %G%
 * @author	Arthur van Hoff, Werner Randelshofer
 */
public interface ServiceListener extends EventListener
{
    /**
     * A service has been added.
     *
     * @param event The ServiceEvent providing the name and fully qualified type
     *              of the service.
     */
    void serviceAdded(ServiceEvent event);

    /**
     * A service has been removed.
     *
     * @param event The ServiceEvent providing the name and fully qualified type
     *              of the service.
     */
    void serviceRemoved(ServiceEvent event);

    /**
     * A service has been resolved. Its details are now available in the
     * ServiceInfo record.
     *
     * @param event The ServiceEvent providing the name, the fully qualified
     *              type of the service, and the service info record, or null if the service
     *              could not be resolved.
     */
    void serviceResolved(ServiceEvent event);
}
