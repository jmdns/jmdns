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

import java.io.*;
import java.net.*;
import java.util.*;

// REMIND: unicast reply
// REMIND: multiple IP addresses

/**
 * mDNS implementation in Java.
 *
 * @author	Arthur van Hoff
 * @version 	%I%, %G%
 */
public class JmDNS extends DNSConstants
{
    static int debug = Integer.parseInt(System.getProperty("jmdns.debug", "0"));
    // static int debug = 1;

    InetAddress group;
    MulticastSocket socket;
    Vector listeners;
    Vector browsers;
    DNSCache cache;
    Hashtable services;
    Thread shutdown;
    boolean done;
    DNSRecord.Address host;

    /**
     * Create an instance of JmDNS.
     */
    public JmDNS() throws IOException
    {
	try {
	    InetAddress addr = InetAddress.getLocalHost();
	    init(addr, addr.getHostName());
	} catch (IOException e) {
	    init(null, "computer");
	}
    }

    /**
     * Create an instance of JmDNS and bind it to a
     * specific network interface given its IP-address.
     */
    public JmDNS(InetAddress addr) throws IOException
    {
	init(addr, addr.getHostName());
    }

    /**
     * Initialize everything.
     */
    void init(InetAddress intf, String name) throws IOException
    {
	if (!name.endsWith(".")) {
	    name += ".local.";
	}
	group = InetAddress.getByName(MDNS_GROUP);
	socket = new MulticastSocket(MDNS_PORT);
	if (intf != null) {
	    socket.setInterface(intf);
	}
	socket.setTimeToLive(255);
	socket.joinGroup(group);

	cache = new DNSCache(100);
	listeners = new Vector();
	browsers = new Vector();
	services = new Hashtable(20);

	new Thread(new SocketListener(), "JmDNS.SocketListener").start();
	new Thread(new RecordReaper(), "JmDNS.RecordReaper").start();
	shutdown = new Thread(new Shutdown(), "JmDNS.Shutdown");
	Runtime.getRuntime().addShutdownHook(shutdown);

	// host to IP address binding
	byte data[] = intf.getAddress();
	int ip = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
	host = new DNSRecord.Address(name, TYPE_A, CLASS_IN, 30*60, ip);

	// REMIND: deal with conflicts
    }

    /**
     * Get service information. If the information is not cached, the method
     * will block until updated informatin is received.
     *
     * @param type full qualified service type, such as <code>_http._tcp.local.</code>.
     * @param name full qualified service name, such as <code>foobar._http._tcp.local.</code>.
     * @return null if the service information cannot be obtained
     */
    public ServiceInfo getServiceInfo(String type, String name)
    {
	return getServiceInfo(type, name, 3*1000);
    }

    /**
     * Get service information. If the information is not cached, the method
     * will block for the given timeout until updated informatin is received.
     *
     * @param type full qualified service type, such as <code>_http._tcp.local.</code>.
     * @param name full qualified service name, such as <code>foobar._http._tcp.local.</code>.
     * @param timeout timeout in milliseconds
     * @return null if the service information cannot be obtained
     */
    public ServiceInfo getServiceInfo(String type, String name, int timeout)
    {
	ServiceInfo info = new ServiceInfo(type, name);
	return info.request(this, timeout) ? info : null;
    }

    /**
     * Listen for services of a given type. The type has to be a fully qualified
     * type name such as <code>_http._tcp.local.</code>.
     * @param type full qualified service type, such as <code>_http._tcp.local.</code>.
     get* @param listener listener for service updates
     */
    public synchronized void addServiceListener(String type, ServiceListener listener)
    {
	removeServiceListener(listener);
	browsers.addElement(new ServiceBrowser(type, listener));
    }

    /**
     * Remove listener for services of a given type.
     * @param listener listener for service updates
     */
    public synchronized void removeServiceListener(ServiceListener listener)
    {
	for (int i = browsers.size() ; i-- > 0 ;) {
	    ServiceBrowser browser = (ServiceBrowser)browsers.elementAt(i);
	    if (browser.listener == listener) {
		browsers.removeElementAt(i);
		browser.close();
		return;
	    }
	}
    }

    /**
     * Register a service. The service is registered for access by other jmdns clients.
     * The name of the service may be changed to make it unique.
     */
    public synchronized void registerService(ServiceInfo info) throws IOException
    {
	// bind the service to this address
	info.server = host.name;
	info.addr = host.getInetAddress();

	try {
	    // check for a unqiue name
	    checkService(info);

	    // add the service
	    services.put(info.name, info);

	    // announce the service
	    long now = System.currentTimeMillis();
	    long nextTime = now;
	    for (int i = 0 ; i < 3 ;) {
		if (now < nextTime) {
		    wait(nextTime - now);
		    now = System.currentTimeMillis();
		    continue;
		}

		DNSOutgoing out = new DNSOutgoing(FLAGS_QR_RESPONSE | FLAGS_AA);
		out.addAnswer(new DNSRecord.Pointer(info.type, TYPE_PTR, CLASS_IN, 60, info.name), 0);
		out.addAnswer(new DNSRecord.Service(info.name, TYPE_SRV, CLASS_IN, 60, info.priority, info.weight, info.port, host.name), 0);
		out.addAnswer(new DNSRecord.Text(info.name, TYPE_TXT, CLASS_IN, 60, info.text), 0);
		out.addAnswer(host, 0);
		send(out);
		i++;
		nextTime += 225;
	    }
	} catch (InterruptedException e) {
	    throw new IOException("interrupted I/O");
	}
    }

    /**
     * Unregister a service. The service should have been registered.
     */
    public synchronized void unregisterService(ServiceInfo info)
    {
	try {
	    services.remove(info.name);

	    // unregister the service
	    long now = System.currentTimeMillis();
	    long nextTime = now;
	    for (int i = 0 ; i < 3 ; ) {
		if (now < nextTime) {
		    wait(nextTime - now);
		    now = System.currentTimeMillis();
		    continue;
		}
		DNSOutgoing out = new DNSOutgoing(FLAGS_QR_RESPONSE | FLAGS_AA);
		out.addAnswer(new DNSRecord.Pointer(info.type, TYPE_PTR, CLASS_IN, 0, info.name), 0);
		send(out);
		i++;
		nextTime += 125;
	    }
	} catch (IOException e) {
	    // ignore
	} catch (InterruptedException e) {
	    // ignore
	}
    }

    /**
     * Unregister a service.
     */
    public synchronized void unregisterAllServices()
    {
	if (services.size() == 0) {
	    return;
	}

	try {
	    // unregister all services
	    long now = System.currentTimeMillis();
	    long nextTime = now;
	    for (int i = 0 ; i < 3 ; ) {
		if (now < nextTime) {
		    wait(nextTime - now);
		    now = System.currentTimeMillis();
		    continue;
		}
		DNSOutgoing out = new DNSOutgoing(FLAGS_QR_RESPONSE | FLAGS_AA);
		for (Enumeration e = services.elements() ; e.hasMoreElements() ;) {
		    ServiceInfo info = (ServiceInfo)e.nextElement();
		    out.addAnswer(new DNSRecord.Pointer(info.type, TYPE_PTR, CLASS_IN, 0, info.name), 0);
		}
		send(out);
		i++;
		nextTime += 125;
	    }
	} catch (IOException e) {
	    // ignore
	} catch (InterruptedException e) {
	    // ignore
	}
    }

    /**
     * Check that a service name is unique.
     */
    void checkService(ServiceInfo info) throws IOException, InterruptedException
    {
	long now = System.currentTimeMillis();
	long nextTime = now;
	for (int i = 0 ; i < 3 ;) {
	    for (Iterator j = cache.find(info.type) ; j.hasNext() ;) {
		DNSRecord a = (DNSRecord)j.next();
		if ((a.type == TYPE_PTR) && !a.isExpired(now) && info.name.equals(((DNSRecord.Pointer)a).alias)) {
		    // collision, uniquify using the IP address
		    if (info.getName().indexOf('.') < 0) {
			info.name = info.getName() + ".[" + Integer.toHexString(info.getIPAddress()) + ":" + info.port + "]." + info.type;
			checkService(info);
			return;
		    }
		    throw new IOException("failed to pick unique service name");
		}
	    }
	    if (now < nextTime) {
		wait(nextTime - now);
		now = System.currentTimeMillis();
		continue;
	    }
	    DNSOutgoing out = new DNSOutgoing(FLAGS_QR_QUERY | FLAGS_AA);
	    out.addQuestion(new DNSQuestion(info.type, TYPE_PTR, CLASS_IN));
	    out.addAuthorativeAnswer(new DNSRecord.Pointer(info.type, TYPE_PTR, CLASS_IN, 60, info.name));
	    send(out);
	    i++;
	    nextTime += 175;
	}
    }

    /**
     * Listener for record updates.
     */
    static abstract class Listener extends DNSConstants {
	/**
	 * Update a DNS record.
	 */
	abstract void updateRecord(JmDNS jmdns, long now, DNSRecord record);
    }

    /**
     * Add a listener for a question. The listener will receive updates to
     * of answers to the question as they arrive, or from the cache if they
     * are already available.
     */
    synchronized void addListener(Listener listener, DNSQuestion question)
    {
	long now = System.currentTimeMillis();

	// add the new listener
	listeners.addElement(listener);

	// report existing matched records
	if (question != null) {
	    for (Iterator i = cache.find(question.name) ; i.hasNext() ; ) {
		DNSRecord c = (DNSRecord)i.next();
		if (question.answeredBy(c) && !c.isExpired(now)) {
		    listener.updateRecord(this, now, c);
		}
	    }
	}
	notifyAll();
    }

    /**
     * Remove a listener from all outstanding questions. The listener will no longer
     * receive any updates.
     */
    synchronized void removeListener(Listener listener)
    {
	listeners.removeElement(listener);
	notifyAll();
    }

    /**
     * Notify all listeners that a record was updated.
     */
    synchronized void updateRecord(long now, DNSRecord rec)
    {
	for (Enumeration e = listeners.elements() ; e.hasMoreElements() ;) {
	    Listener listener = (Listener)e.nextElement();
	    listener.updateRecord(this, now, rec);
	}
	notifyAll();
    }

    /**
     * Handle an incoming response. Cache answers, and pass them on to
     * the appropriate questions.
     */
    synchronized void handleResponse(DNSIncoming msg) throws IOException
    {
	long now = System.currentTimeMillis();

	for (Enumeration e = msg.answers.elements() ; e.hasMoreElements() ;) {
	    DNSRecord rec = (DNSRecord)e.nextElement();
	    boolean expired = rec.isExpired(now);

	    // update the cache
	    DNSRecord c = (DNSRecord)cache.get(rec);
	    if (c != null) {
		if (expired) {
		    cache.remove(c);
		} else {
		    c.resetTTL(rec);
		    rec = c;
		}
	    } else if (!expired) {
		cache.add(rec);
	    }

	    // notify the listeners
	    updateRecord(now, rec);
	}
    }

    /**
     * Handle an incoming query. See if we can answer any part of it
     * given our registered records.
     */
    synchronized void handleQuery(DNSIncoming in, InetAddress addr, int port) throws IOException
    {
	DNSOutgoing out = null;

	// REMIND: handle meta query for service types

	// for unicast responses the question must be included
	if (port != MDNS_PORT) {
	    out = new DNSOutgoing(FLAGS_QR_RESPONSE | FLAGS_AA, false);
	    for (Enumeration e = in.questions.elements() ; e.hasMoreElements() ;) {
		out.addQuestion((DNSQuestion)e.nextElement());
	    }
	}

	// answer relevant questions
	for (Enumeration e = in.questions.elements() ; e.hasMoreElements() ;) {
	    DNSQuestion q = (DNSQuestion)e.nextElement();
	    switch (q.type) {
	      case TYPE_A:
		// address request
		if (q.name.equals(host.name)) {
		    if (out == null) {
			out = new DNSOutgoing(FLAGS_QR_RESPONSE | FLAGS_AA);
		    }
		    out.addAnswer(in, host);
		}
		break;

	      case TYPE_PTR:
		// find matching services
		for (Enumeration s = services.elements() ; s.hasMoreElements() ; ) {
		    ServiceInfo info = (ServiceInfo)s.nextElement();
		    if (q.name.equals(info.type)) {
			if (out == null) {
			    out = new DNSOutgoing(FLAGS_QR_RESPONSE | FLAGS_AA);
			}
			out.addAnswer(in, new DNSRecord.Pointer(info.type, TYPE_PTR, CLASS_IN, 60, info.name));
		    }
		}
		break;

	      default:
		// find service
		ServiceInfo info = (ServiceInfo)services.get(q.name);
		if (info != null) {
		    if (out == null) {
			out = new DNSOutgoing(FLAGS_QR_RESPONSE | FLAGS_AA);
		    }
		    if ((q.type == TYPE_SRV) || (q.type == TYPE_ANY)) {
			out.addAnswer(in, new DNSRecord.Service(info.name, TYPE_SRV, CLASS_IN | CLASS_UNIQUE, 60, info.priority, info.weight, info.port, host.name));
		    }
		    if ((q.type == TYPE_TXT) || (q.type == TYPE_ANY)) {
			out.addAnswer(in, new DNSRecord.Text(info.name, TYPE_TXT, CLASS_IN | CLASS_UNIQUE, 60, info.text));
		    }
		}
	    } 
	}
	// REMIND: add service info if there is space
	if ((out != null) && (out.numAnswers > 0)) {
	    out.id = in.id;
	    out.finish();
	    socket.send(new DatagramPacket(out.data, out.off, addr, port));
	}
    }

    /**
     * Send a message an outgoing DNS message.
     */
    synchronized void send(DNSOutgoing out) throws IOException
    {
	out.finish();
	socket.send(new DatagramPacket(out.data, out.off, group, MDNS_PORT));
    }

    /**
     * Listen for multicast packets.
     */
    class SocketListener implements Runnable
    {
	public void run()
	{
	    try {
		byte buf[] = new byte[MAX_MSG_ABSOLUTE];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while (!done) {
		    packet.setLength(buf.length);
		    socket.receive(packet);
		    if (done) {
			break;
		    }
		    try {
			DNSIncoming msg = new DNSIncoming(packet);
			if (debug > 0) {
			    msg.print(debug > 1);
			    System.out.println();
			}
			if (msg.isQuery()) {
			    if (packet.getPort() != MDNS_PORT) {
				handleQuery(msg, packet.getAddress(), packet.getPort());
			    }
			    handleQuery(msg, group, MDNS_PORT);
			} else {
			    handleResponse(msg);
			}
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
	    } catch (IOException e) {
		if (!done) {
		    e.printStackTrace();
		}
	    }
	}
    }

    /**
     * Schedule questions
     */
    class RecordReaper implements Runnable
    {
	public void run()
	{

	    try {
		synchronized (JmDNS.this) {
		    while (true) {
			JmDNS.this.wait(10 * 1000);
			if (done) {
			    return;
			}

			// remove expired answers from the cache
			long now = System.currentTimeMillis();
			for (Iterator i = cache.all() ; i.hasNext() ; ) {
			    DNSRecord c = (DNSRecord)i.next();
			    if (c.isExpired(now)) {
				updateRecord(now, c);
				i.remove();
			    }
			}
		    }
		}
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

    /**
     * Browse for a service of a given type
     */
    class ServiceBrowser extends JmDNS.Listener implements Runnable
    {
	String type;
	ServiceListener listener;
	Hashtable services;
	long nextTime;
	int delay;
	boolean done;
	LinkedList list;
    
	/**
	 * Create a browser for a service type.
	 */
	ServiceBrowser(String type, ServiceListener listener)
	{
	    this.type = type;
	    this.listener = listener;
	    this.services = new Hashtable();
	    this.nextTime = System.currentTimeMillis();
	    this.delay = 500;
	    this.list = new LinkedList();

	    addListener(this, new DNSQuestion(type, TYPE_PTR, CLASS_IN));
	    new Thread(this, "JmDNS.ServiceBrowser: " + type).start();
	}


	/**
	 * Event for notifying a service listener.
	 */
	abstract class Event
	{
	    String name;

	    Event(String name)
	    {
		this.name = name;
	    }
	    abstract void send();
	}

	/**
	 * Update a record.
	 */
	void updateRecord(JmDNS jmdns, long now, DNSRecord rec)
	{
	    if ((rec.type == TYPE_PTR) && rec.name.equals(type)) {
		boolean expired = rec.isExpired(now);
		String name = ((DNSRecord.Pointer)rec).alias;
		DNSRecord old = (DNSRecord)services.get(name);
		if ((old == null) && !expired) {
		    // new record
		    services.put(name, rec);
		    list.addLast(new Event(name) {
			    void send() {listener.addService(JmDNS.this, type, this.name);}
			});
		} else if ((old != null) && !expired) {
		    // update record
		    old.resetTTL(rec);
		} else if ((old != null) && expired) {
		    // expire record
		    services.remove(name);
		    list.addLast(new Event(name) {
			    void send() {listener.removeService(JmDNS.this, type, this.name);}
			});
		    return;
		}

		// adjust next request time
		long expires = rec.getExpirationTime(75);
		if (expires < nextTime) {
		    nextTime = rec.getExpirationTime(75);
		}
	    }
	}

	/**
	 * Request.
	 */
	public void run()
	{
	    try {
		while (true) {
		    Event evt = null;
		    
		    synchronized (JmDNS.this) {
			long now = System.currentTimeMillis();
			if ((list.size() == 0) && (nextTime > now)) {
			    JmDNS.this.wait(nextTime - now);
			}
			if (done) {
			    return;
			}
			now = System.currentTimeMillis();

			// send query
			if (nextTime <= now) {
			    DNSOutgoing out = new DNSOutgoing(FLAGS_QR_QUERY);
			    out.addQuestion(new DNSQuestion(type, TYPE_PTR, CLASS_IN));
			    for (Enumeration e = services.elements() ; e.hasMoreElements() ;) {
				DNSRecord rec = (DNSRecord)e.nextElement();
				if (!rec.isExpired(now)) {
				    out.addAnswer(rec, now);
				}
			    }
			    send(out);

			    // schedule the next one
			    nextTime = now + delay;
			    delay = Math.min(20*1000, delay * 2);
			}

			// get the next event
			if (list.size() > 0) {
			    evt = (Event)list.removeFirst();
			}
		    }
		    if (evt != null) {
			evt.send();
		    }
		}
	    } catch (IOException e) {
		e.printStackTrace();
	    } catch (InterruptedException e) {
		// oops
	    }
	}

	void close()
	{
	    synchronized (JmDNS.this) {
		if (!done) {
		    done = true;
		    removeListener(this);
		}
	    }
	}
    }

    /**
     * Shutdown operations.
     */
    class Shutdown implements Runnable
    {
	public void run()
	{
	    shutdown = null;
	    close();
	}
    }

    /**
     * Close down jmdns. Release all resources and unregister all services.
     */
    public synchronized void close()
    {
	if (!done) {
	    done = true;
	    notifyAll();

	    // remove the shutdown hook
	    if (shutdown != null) {
		Runtime.getRuntime().removeShutdownHook(shutdown);
	    }

	    // unregister services
	    unregisterAllServices();

	    // close socket
	    try {
		socket.leaveGroup(group);
		socket.close();
	    } catch (IOException e) {
		// ignore
	    }
	}
    }

    /**
     * List cache entries, for debugging only.
     */
    void print()
    {
	if (cache.count > 0) {
	    System.out.println("---- cache ----");
	    cache.print();
	    System.out.println();
	}
    }
}
