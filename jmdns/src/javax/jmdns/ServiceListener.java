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

/**
 * Listener for service updates.
 *
 * @author	Arthur van Hoff
 * @version 	%I%, %G%
 */
public interface ServiceListener {
    /**
     * A service is added.
     * @param type the fully qualified type of the service
     * @param name the fully qualified name of the service
     */
    void addService(JmDNS jmdns, String type, String name);

    /**
     * A service is removed.
     * @param type the fully qualified type of the service
     * @param name the fully qualified name of the service
     */
    void removeService(JmDNS jmdns, String type, String name);

    /**
     * A service is resolved. Its details are now available in the ServiceInfo record.
     * @param type the fully qualified type of the service
     * @param name the fully qualified name of the service
     * @param info the service info record, or null if the service could be be resolved
     */
    void resolveService(JmDNS jmdns, String type, String name, ServiceInfo info);
}
