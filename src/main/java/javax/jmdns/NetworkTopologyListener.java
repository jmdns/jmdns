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
/**
 *
 */
package javax.jmdns;

import java.util.EventListener;

/**
 * Listener for network topology updates.
 * 
 * @author C&eacute;drik Lime, Pierre Frisch
 */
public interface NetworkTopologyListener extends EventListener {
    /**
     * A network address has been added.<br/>
     * 
     * @param event
     *            The NetworkTopologyEvent providing the name and fully qualified type of the service.
     */
    void inetAddressAdded(NetworkTopologyEvent event);

    /**
     * A network address has been removed.
     * 
     * @param event
     *            The NetworkTopologyEvent providing the name and fully qualified type of the service.
     */
    void inetAddressRemoved(NetworkTopologyEvent event);

}
