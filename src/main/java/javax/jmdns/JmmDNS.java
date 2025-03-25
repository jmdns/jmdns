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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.jmdns.impl.JmmDNSImpl;

/**
 * <p>
 * Java Multihomed Multicast DNS
 * </p>
 * Uses an underlying {@link JmDNS} instance for each {@link InetAddress} found on this computer.<br/>
 * This class will monitor network topology changes, and will create or destroy JmDNS instances as required. It is your responsibility to maintain services registration (hint: use a {@link NetworkTopologyListener}).<br/>
 * Most of this class methods have no notion of transaction: if an Exception is raised in the middle of execution, you may be in an incoherent state.
 * <p>
 * <b>Note:</b> This API is experimental and may change in the future please let us know what work and what does not work in your application.
 * </p>
 *
 * @author C&eacute;drik Lime, Pierre Frisch
 */
public interface JmmDNS extends Closeable {

    /**
     * JmmDNS.Factory enable the creation of new instance of JmmDNS.
     */
    final class Factory {
        private static volatile JmmDNS _instance;

        /**
         * This interface defines a delegate to the EOClassDescriptionRegister class to enable subclassing.
         */
        public interface ClassDelegate {

            /**
             * Allows the delegate the opportunity to construct and return a different JmmDNS.
             *
             * @return Should return a new JmmDNS.
             * @see #classDelegate()
             * @see #setClassDelegate(ClassDelegate anObject)
             */
            JmmDNS newJmmDNS();

        }

        private static final AtomicReference<ClassDelegate> _databaseClassDelegate = new AtomicReference<>();

        private Factory() {
            super();
        }

        /**
         * Assigns <code>delegate</code> as JmmDNS's class delegate. The class delegate is optional.
         *
         * @param delegate
         *            The object to set as JmmDNS's class delegate.
         * @see #classDelegate()
         * @see JmmDNS.Factory.ClassDelegate
         */
        public static void setClassDelegate(ClassDelegate delegate) {
            _databaseClassDelegate.set(delegate);
        }

        /**
         * Returns JmmDNS's class delegate.
         *
         * @return JmmDNS's class delegate.
         * @see #setClassDelegate(ClassDelegate anObject)
         * @see JmmDNS.Factory.ClassDelegate
         */
        public static ClassDelegate classDelegate() {
            return _databaseClassDelegate.get();
        }

        /**
         * Returns a new instance of JmmDNS using the class delegate if it exists.
         *
         * @return new instance of JmmDNS
         */
        private static JmmDNS newJmmDNS() {
            JmmDNS dns = null;
            ClassDelegate delegate = _databaseClassDelegate.get();
            if (delegate != null) {
                dns = delegate.newJmmDNS();
            }
            return (dns != null ? dns : new JmmDNSImpl());
        }

        /**
         * Return the instance of the Multihomed Multicast DNS.
         *
         * @return the JmmDNS
         */
        public static JmmDNS getInstance() {
            synchronized (Factory.class) {
                if (_instance == null) {
                    _instance = JmmDNS.Factory.newJmmDNS();
                }
                return _instance;
            }
        }

        /**
         * Closes the instance if still running and discard it.
         *
         * @throws IOException if any IO error occurs
         */
        public static void close() throws IOException {
            synchronized (Factory.class) {
                if (_instance == null) {
                    return;
                }
                _instance.close();
                _instance = null;
            }
        }
    }

    /**
     * Return the names of the JmDNS instances.
     *
     * @return String[] of name of the JmDNS
     * @see javax.jmdns.JmDNS#getName()
     */
    String[] getNames();

    /**
     * Return the list HostName associated with this JmmDNS instance.
     *
     * @return String[] of host names
     * @see javax.jmdns.JmDNS#getHostName()
     */
    String[] getHostNames();

    /**
     * Return the list of addresses of the interface to which this instance of JmmDNS is bound.
     *
     * @return InetAddress[] of Internet Address
     * @exception IOException if any IO error occurs
     * @see javax.jmdns.JmDNS#getInetAddress()
     */
    InetAddress[] getInetAddresses() throws IOException;

    /**
     * Return the list of addresses of the interface to which this instance of JmmDNS is bound.
     *
     * @return InetAddress[] of Internet Address
     * @exception IOException if any IO error occurs
     * @see javax.jmdns.JmDNS#getInterface()
     * @deprecated do not use this implementation yields unpredictable results use {@link #getInetAddresses()}
     */
    @Deprecated
    InetAddress[] getInterfaces() throws IOException;

    /**
     * Return a list of all the registered JmDNS instances
     *
     * @return JmDNS[] of JmDNS instances
     */
    JmDNS[] getDNS();

    /**
     * Get service information. If the information is not cached, the method will block until updated information is received on all DNS.
     * <p/>
     * Usage note: Do not call this method from the AWT event dispatcher thread. You will make the user interface unresponsive.
     *
     * @param type
     *            fully qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @return ServiceInfo[] list of service info. If no service info is found the list is empty.
     * @see javax.jmdns.JmDNS#getServiceInfo(java.lang.String, java.lang.String)
     */
    ServiceInfo[] getServiceInfos(String type, String name);

    /**
     * Get service information. If the information is not cached, the method will block until updated information is received on all DNS.
     * <p/>
     * Usage note: If you call this method from the AWT event dispatcher thread, use a small timeout, or you will make the user interface unresponsive.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param timeout
     *            timeout in milliseconds. Typical timeout should be 5s.
     * @return ServiceInfo[] list of service info. If no service info is found the list is empty.
     * @see javax.jmdns.JmDNS#getServiceInfo(java.lang.String, java.lang.String, long)
     */
    ServiceInfo[] getServiceInfos(String type, String name, long timeout);

    /**
     * Get service information. If the information is not cached, the method will block until updated information is received on all DNS.
     * <p/>
     * Usage note: If you call this method from the AWT event dispatcher thread, use a small timeout, or you will make the user interface unresponsive.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param persistent
     *            if <code>true</code> ServiceListener.resolveService will be called whenever new information is received.
     * @return ServiceInfo[] of service info. If no service info is found the list is empty.
     * @see javax.jmdns.JmDNS#getServiceInfo(java.lang.String, java.lang.String, boolean)
     */
    ServiceInfo[] getServiceInfos(String type, String name, boolean persistent);

    /**
     * Get service information. If the information is not cached, the method will block until updated information is received on all DNS.
     * <p/>
     * Usage note: If you call this method from the AWT event dispatcher thread, use a small timeout, or you will make the user interface unresponsive.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param timeout
     *            timeout in milliseconds. Typical timeout should be 5s.
     * @param persistent
     *            if <code>true</code> ServiceListener.resolveService will be called whenever new information is received.
     * @return ServiceInfo[] list of service info. If no service info is found the list is empty.
     * @see javax.jmdns.JmDNS#getServiceInfo(java.lang.String, java.lang.String, boolean, long)
     */
    ServiceInfo[] getServiceInfos(String type, String name, boolean persistent, long timeout);

    /**
     * Request service information. The information about the service is requested and the ServiceListener.resolveService method is called as soon as it is available.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @see javax.jmdns.JmDNS#requestServiceInfo(java.lang.String, java.lang.String)
     */
    void requestServiceInfo(String type, String name);

    /**
     * Request service information. The information about the service is requested and the ServiceListener.resolveService method is called as soon as it is available.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param persistent
     *            if <code>true</code> ServiceListener.resolveService will be called whenever new information is received.
     * @see javax.jmdns.JmDNS#requestServiceInfo(java.lang.String, java.lang.String, boolean)
     */
    void requestServiceInfo(String type, String name, boolean persistent);

    /**
     * Request service information. The information about the service is requested and the ServiceListener.resolveService method is called as soon as it is available.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param timeout
     *            timeout in milliseconds
     * @see javax.jmdns.JmDNS#requestServiceInfo(java.lang.String, java.lang.String, long)
     */
    void requestServiceInfo(String type, String name, long timeout);

    /**
     * Request service information. The information about the service is requested and the ServiceListener.resolveService method is called as soon as it is available.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param persistent
     *            if <code>true</code> ServiceListener.resolveService will be called whenever new information is received.
     * @param timeout
     *            timeout in milliseconds
     * @see javax.jmdns.JmDNS#requestServiceInfo(java.lang.String, java.lang.String, boolean, long)
     */
    void requestServiceInfo(String type, String name, boolean persistent, long timeout);

    /**
     * Listen for service types.
     *
     * @param listener
     *            listener for service types
     * @exception IOException if any IO error occurs
     * @see javax.jmdns.JmDNS#addServiceTypeListener(javax.jmdns.ServiceTypeListener)
     */
    void addServiceTypeListener(ServiceTypeListener listener) throws IOException;

    /**
     * Remove listener for service types.
     *
     * @param listener
     *            listener for service types
     * @see javax.jmdns.JmDNS#removeServiceTypeListener(javax.jmdns.ServiceTypeListener)
     */
    void removeServiceTypeListener(ServiceTypeListener listener);

    /**
     * Listen for services of a given type. The type has to be a fully qualified type name such as <code>_http._tcp.local.</code>.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code>.
     * @param listener
     *            listener for service updates
     * @see javax.jmdns.JmDNS#addServiceListener(java.lang.String, javax.jmdns.ServiceListener)
     */
    void addServiceListener(String type, ServiceListener listener);

    /**
     * Remove listener for services of a given type.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code>.
     * @param listener
     *            listener for service updates
     * @see javax.jmdns.JmDNS#removeServiceListener(java.lang.String, javax.jmdns.ServiceListener)
     */
    void removeServiceListener(String type, ServiceListener listener);

    /**
     * Register a service. The service is registered for access by other jmdns clients. The name of the service may be changed to make it unique.<br>
     * <b>Note</b> the Service info is cloned for each network interface.
     *
     * @param info
     *            service info to register
     * @exception IOException if any IO error occurs
     * @see javax.jmdns.JmDNS#registerService(javax.jmdns.ServiceInfo)
     */
    void registerService(ServiceInfo info) throws IOException;

    /**
     * Unregister a service. The service should have been registered.
     *
     * @param info
     *            service info to remove
     * @see javax.jmdns.JmDNS#unregisterService(javax.jmdns.ServiceInfo)
     */
    void unregisterService(ServiceInfo info);

    /**
     * Unregister all services.
     *
     * @see javax.jmdns.JmDNS#unregisterAllServices()
     */
    void unregisterAllServices();

    /**
     * Register a service type. If this service type was not already known, all service listeners will be notified of the new service type. Service types are automatically registered as they are discovered.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code>.
     * @see javax.jmdns.JmDNS#registerServiceType(java.lang.String)
     */
    void registerServiceType(String type);

    /**
     * Returns a list of service infos of the specified type.
     *
     * @param type
     *            Service type name, such as <code>_http._tcp.local.</code>.
     * @return An array of service instance.
     * @see javax.jmdns.JmDNS#list(java.lang.String)
     */
    ServiceInfo[] list(String type);

    /**
     * Returns a list of service infos of the specified type.
     *
     * @param type
     *            Service type name, such as <code>_http._tcp.local.</code>.
     * @param timeout
     *            timeout in milliseconds. Typical timeout should be 6s.
     * @return An array of service instance.
     * @see javax.jmdns.JmDNS#list(java.lang.String, long)
     */
    ServiceInfo[] list(String type, long timeout);

    /**
     * Returns a list of service infos of the specified type sorted by subtype. Any service that do not register a subtype is listed in the empty subtype section.
     *
     * @param type
     *            Service type name, such as <code>_http._tcp.local.</code>.
     * @return A dictionary of service info by subtypes.
     * @see javax.jmdns.JmDNS#listBySubtype(java.lang.String)
     */
    Map<String, ServiceInfo[]> listBySubtype(String type);

    /**
     * Returns a list of service infos of the specified type sorted by subtype. Any service that do not register a subtype is listed in the empty subtype section.
     *
     * @param type
     *            Service type name, such as <code>_http._tcp.local.</code>.
     * @param timeout
     *            timeout in milliseconds. Typical timeout should be 6s.
     * @return A dictionary of service info by subtypes.
     * @see javax.jmdns.JmDNS#listBySubtype(java.lang.String, long)
     */
    Map<String, ServiceInfo[]> listBySubtype(String type, long timeout);

    /**
     * Listen to network changes.
     *
     * @param listener
     *            listener for network changes
     */
    void addNetworkTopologyListener(NetworkTopologyListener listener);

    /**
     * Remove listener for network changes.
     *
     * @param listener
     *            listener for network changes
     */
    void removeNetworkTopologyListener(NetworkTopologyListener listener);

    /**
     * Returns list of network change listeners
     *
     * @return NetworkTopologyListener[] list of network change listeners
     */
    NetworkTopologyListener[] networkListeners();

}
