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

import java.util.EventListener;

/**
 * Listener for service types.
 * 
 * @author Arthur van Hoff, Werner Randelshofer
 */
public interface ServiceTypeListener extends EventListener {
    /**
     * A new service type was discovered.
     * 
     * @param event
     *            The service event providing the fully qualified type of the service.
     */
    void serviceTypeAdded(ServiceEvent event);

    /**
     * A new subtype for the service type was discovered.
     * 
     * <pre>
     * &lt;sub&gt;._sub.&lt;app&gt;.&lt;protocol&gt;.&lt;servicedomain&gt;.&lt;parentdomain&gt;.
     * </pre>
     * 
     * @param event
     *            The service event providing the fully qualified type of the service with subtype.
     * @since 3.2.0
     */
    void subTypeForServiceTypeAdded(ServiceEvent event);
}
