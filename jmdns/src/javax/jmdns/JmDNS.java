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

// REMIND: multiple IP addresses

/**
 * mDNS implementation in Java.
 *
 * @author	Arthur van Hoff, Rick Blair, Jeff Sonstein
 * @version 	%I%, %G%
 */
public class JmDNS 
{

  public static String VERSION = "0.3";
  // made debug public so apps can check and decide to do their own logging and so on
  public static int debug = Integer.parseInt(System.getProperty("jmdns.debug", "0"));

  // made all instance variables below here private - jeffs
  // added package protected method getCache() and changed calls from ServiceInfo
  private InetAddress group;
  private MulticastSocket socket;
  private Vector listeners;
  private Vector browsers;
  private Vector typeListeners;
  private DNSCache cache, queryCache;  //queryCache For conflict detection.  Trading Space for Speed.
  
  private Hashtable services;
  private Thread shutdown;
  private Probe prober;
  private boolean done;
  private SocketListener incomingListener = null;
  private boolean linklocal;
  private boolean loopback;
  private DNSRecord.Address host;
  private Hashtable serviceTypes;
  private int _ip;  //

  /**
   * Create an instance of JmDNS.
   */
  public JmDNS() throws IOException
  {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      init(isLoopback(addr) ? null : addr, addr.getHostName());
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
    //A host name with "." is illegal. so strip off everything and append .local.
    int idx = name.indexOf(".");
    if(idx > 0)
      name = name .substring(0, idx);
    name += ".local.";

    group = InetAddress.getByName(DNSConstants.MDNS_GROUP);
    socket = new MulticastSocket(DNSConstants.MDNS_PORT);
    if (intf != null) {
      socket.setInterface(intf);
    }
    socket.setTimeToLive(255);
    socket.joinGroup(group);
    loopback = isLoopback(intf);
    linklocal = isLinkLocal(intf);

    cache = new DNSCache(100);

    //must check incoming queries for duplicate probes.  Then resolve conflicts.
    //setup the listener in probe mode which then caches queries.
    //the probe code then checks the queryCache as well as the normal cache for conflicts.
    //See section 9.1 and 9.2 of the draft-cheshire-dnsext-multcastdns-04.txt memo.
    //This could be done with a local hashtable, but we already search the other cache.

    queryCache = new DNSCache(20);

    listeners = new Vector();
    browsers = new Vector();
    typeListeners = new Vector();
    services = new Hashtable(20);
    serviceTypes = new Hashtable(20);


    // host to IP address binding
    byte data[] = intf.getAddress();
    _ip = ((data[0] & 0xFF) << 24) | 
      ((data[1] & 0xFF) << 16) | 
      ((data[2] & 0xFF) <<  8) | 
      (data[3] & 0xFF);

    DNSRecord.Address host = new DNSRecord.Address(name, DNSConstants.TYPE_A, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, _ip);
    prober = new Probe();
    //As per draft-cheshire-dnsext-multicastdns-04.txt section 9
    //Probe then announce.
    //Host is side-effected.
    prober.probe(host);
    prober.announce(host);
    // Already started by the probe method.
    //new Thread(new SocketListener(), "JmDNS.SocketListener").start();
    new Thread(new RecordReaper(), "JmDNS.RecordReaper").start();
    shutdown = new Thread(new Shutdown(), "JmDNS.Shutdown");
    //new Thread(prober,"JmDNS.Prober").start();
    Runtime.getRuntime().addShutdownHook(shutdown);
    //All done with the prober so get rid of it
    prober = null;
  }
  
  /**
   * Return the DNSCache associated with the cache variable
   *
   **/
  DNSCache getCache() { 
    return cache;
  }

  /** Return the HostName associated with this JmDNS instance.  
   *  Note: May not be the same as what started.  The host name is subject to 
   *  negotiation.
   **/
  public String getHostName()
  {
    return host.name;
  }

  private void waitInterval(long timeInMillis)
  {
    try 
    {
      Thread.sleep(timeInMillis);
    }   
    catch (InterruptedException e) 
    {
      //Report interrupt.  We could resume wait for what ever the remaining interval
      //but for now just return.
      System.err.println("waitInterval interrupted");
    }
  }


  /**
   * Check if an address is the loopback address
   */
  static boolean isLoopback(InetAddress addr)
  {
    return (addr != null) && addr.getHostAddress().startsWith("127.0.0.1");
  }

  /**
   * Check if an address is linklocal.
   */
  static boolean isLinkLocal(InetAddress addr)
  {
    return (addr != null) && addr.getHostAddress().startsWith("169.254.");
  }

  /**
   * Return the address of the interface to which this instance of JmDNS is bound.
   */
  public InetAddress getInterface() throws IOException
  {
    return socket.getInterface();
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
   * Request service information. The information about the service is requested
   * and the ServiceListener.resolveService method is called as soon as it is available.
   */
  public void requestServiceInfo(String type, String name)
  {
    requestServiceInfo(type, name, 3*1000);
  }

  /**
   * Request service information. The information about the service is requested
   * and the ServiceListener.resolveService method is called as soon as it is available.
   */
  public void requestServiceInfo(String type, String name, int timeout)
  {
    registerServiceType(type);
	
    new Thread(new ServiceResolver(new ServiceInfo(type, name), timeout),
               "JmDNS.ServiceResolver").start();
  }

  /**
   * Listen for service types. 
   * @param listener listener for service types
   */
  public void addServiceTypeListener(ServiceTypeListener listener) throws IOException
  {
    synchronized (this) {
      removeServiceTypeListener(listener);
      typeListeners.addElement(listener);

      // report service types
      for (Enumeration e = serviceTypes.elements() ; e.hasMoreElements() ;) {
        listener.addServiceType(this, (String)e.nextElement());
      }
    }

    try {
      // query for service types
      long now = System.currentTimeMillis();
      long nextTime = now;
      for (int i = 0 ; i < 3 ;) {
        if (now < nextTime) {
          Thread.sleep(nextTime - now);
          now = System.currentTimeMillis();
          continue;
        }
        DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
        out.addQuestion(new DNSQuestion("_services._mdns._udp.local.", DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN));
        for (Enumeration e = serviceTypes.elements() ; e.hasMoreElements(); ) {
          out.addAnswer(new DNSRecord.Pointer("_services._mdns._udp.local.", DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, (String)e.nextElement()), 0);
        }
        send(out);
        i++;
        nextTime += 225;
      }
    } catch (InterruptedException e) {
      throw new IOException("interrupted I/O");
    }
  }

  /**
   * Remove listener for service types.
   * @param listener listener for service types
   */
  public synchronized void removeServiceTypeListener(ServiceTypeListener listener)
  {
    typeListeners.removeElement(listener);
  }

  /**
   * Listen for services of a given type. The type has to be a fully qualified
   * type name such as <code>_http._tcp.local.</code>.
   * @param type full qualified service type, such as <code>_http._tcp.local.</code>.
   * @param listener listener for service updates
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
  public void registerService(ServiceInfo info) throws IOException
  {
    registerServiceType(info.type);

    // bind the service to this address
    info.server = host.name;
    info.addr = host.getInetAddress();

    try {
      synchronized (this) {

        // check for a unqiue name
        // This should be merged with the Host address probing.
        // We also need to put in conflict resolution etc.

        checkService(info);
        // add the service
        services.put(info.name.toLowerCase(), info);
      }

      // announce the service
      long now = System.currentTimeMillis();
      long nextTime = now;
      for (int i = 0 ; i < 3 ;) {
        if (now < nextTime) {
          Thread.sleep(nextTime - now);
          now = System.currentTimeMillis();
          continue;
        }
        DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
        out.addAnswer(new DNSRecord.Pointer(info.type, DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, info.name), 0);
        out.addAnswer(new DNSRecord.Service(info.name, DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, info.priority, info.weight, info.port, host.name), 0);
        out.addAnswer(new DNSRecord.Text(info.name, DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, info.text), 0);
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
  public void unregisterService(ServiceInfo info)
  {
    try {
      services.remove(info.name.toLowerCase());

      // unregister the service
      long now = System.currentTimeMillis();
      long nextTime = now;
      for (int i = 0 ; i < 2 ; ) {
        if (now < nextTime) {
          Thread.sleep(nextTime - now);
          now = System.currentTimeMillis();
          continue;
        }
        DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
        out.addAnswer(new DNSRecord.Pointer(info.type, DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, 0, info.name), 0);
        out.addAnswer(new DNSRecord.Service(info.name, DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN, 0, info.priority, info.weight, info.port, host.name), 0);
        out.addAnswer(new DNSRecord.Text(info.name, DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN, 0, info.text), 0);
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
          Thread.sleep(nextTime - now);
          now = System.currentTimeMillis();
          continue;
        }
        DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
        for (Enumeration e = services.elements() ; e.hasMoreElements() ;) {
          ServiceInfo info = (ServiceInfo)e.nextElement();
          out.addAnswer(new DNSRecord.Pointer(info.type, DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, 0, info.name), 0);
          out.addAnswer(new DNSRecord.Service(info.name, DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN, 0, info.priority, info.weight, info.port, host.name), 0);
          out.addAnswer(new DNSRecord.Text(info.name, DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN, 0, info.text), 0);
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
   * Register a service type. If this service type was not already known,
   * all service listeners will be notified of the new service type. Service types
   * are automatically registered as they are discovered.
   */
  public synchronized void registerServiceType(String type)
  {
    String name = type.toLowerCase();
    if (serviceTypes.get(name) == null) {
      if ((type.indexOf("._mdns._udp.") < 0) && !type.endsWith(".in-addr.arpa.")) {
        serviceTypes.put(name, type);
        for (Enumeration e = typeListeners.elements() ; e.hasMoreElements() ;) {
          ((ServiceTypeListener)e.nextElement()).addServiceType(this, type);
        }
      }
    }
  }

  /**
   * Check that a service name is unique.
   */
  //This should be merged with Probe. 
  void checkService(ServiceInfo info) throws IOException, InterruptedException
  {
    long now = System.currentTimeMillis();
    long nextTime = now;
    for (int i = 0 ; i < 3 ;) {
      for (Iterator j = cache.find(info.type) ; j.hasNext() ;) {
        DNSRecord a = (DNSRecord)j.next();
        if ((a.type == DNSConstants.TYPE_PTR) && !a.isExpired(now) && info.name.equalsIgnoreCase(((DNSRecord.Pointer)a).alias)) {
          String name = info.getName();
          try {
            int l = name.lastIndexOf('(');
            int r = name.lastIndexOf(')');
            if ((l >= 0) && (l < r)) {
              name = name.substring(0, l) + "(" + (Integer.parseInt(name.substring(l+1, r)) + 1) + ")";
            } else {
              name += " (2)";
            }
          } catch (NumberFormatException e) {
            name += " (2)";
          }
          info.name = name + "." + info.type;
          i = 0;
          break;
        }
      }
      //        DNSOutgoing out = new DNSOutgoing(FLAGS_QR_QUERY | FLAGS_AA);
      //        out.addQuestion(new DNSQuestion(info.type, TYPE_PTR, CLASS_IN));
      //        out.addAuthorativeAnswer(new DNSRecord.Pointer(info.type, TYPE_PTR, CLASS_IN, DNS_TTL, info.name));

      DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY | DNSConstants.FLAGS_AA);
      out.addQuestion(new DNSQuestion(info.name, DNSConstants.TYPE_ANY, DNSConstants.CLASS_IN));
      out.addAuthorativeAnswer(new DNSRecord.Service(info.name, DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, info.priority, info.weight, info.port, host.name));

      send(out);
      i++;
      nextTime += DNSConstants.PROBE_WAIT_INTERVAL;

      if (now < nextTime) {
        wait(nextTime - now);
        now = System.currentTimeMillis();
        continue;
      }
    }
  }

  /**
   * Listener for record updates.
   */
  static abstract class Listener {
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

  //return true if conflicts with our own address record.
  //Really needs to be part of the handleResponse method.

  private boolean conflict(DNSIncoming msg) throws IOException
  {
    if(host == null) return false;  // In probe.
    long now = System.currentTimeMillis();
    for (Enumeration e = msg.answers.elements() ; e.hasMoreElements() ;) 
    {
      DNSRecord a = (DNSRecord)e.nextElement();

      boolean expired = a.isExpired(now);
      if  ((a.type == DNSConstants.TYPE_A) && !expired && (((DNSRecord.Address)a).addr != _ip)) 
        //One is enough for a conflict.
      {

        if((host.sameName(a)) && !(host.sameValue(a)))
          return true;
      }
    }
    return false;
  }

  /**
   * Handle an incoming response. Cache answers, and pass them on to
   * the appropriate questions.
   */
  synchronized void handleResponse(DNSIncoming msg) throws IOException
  {
    long now = System.currentTimeMillis();

    for (Enumeration e = msg.answers.elements() ; e.hasMoreElements() ;) 
    {
      DNSRecord rec = (DNSRecord)e.nextElement();
      boolean expired = rec.isExpired(now);

      // update the cache
      DNSRecord c = (DNSRecord)cache.get(rec);
      if (c != null) 
      {
        if (expired) 
        {
          cache.remove(c);
        } 
        else 
        {
          c.resetTTL(rec);
          rec = c;
        }
      } 
      else 
        if (!expired) 
        {
          cache.add(rec);
        }
      switch (rec.type) 
      {
      case DNSConstants.TYPE_PTR:
        // handle _mdns._udp records
        if (rec.name.indexOf("._mdns._udp.") >= 0) 
        {
          if (!expired && rec.name.startsWith("_services._mdns._udp.")) 
          {
            registerServiceType(((DNSRecord.Pointer)rec).alias);
          }
          continue;
        }
        registerServiceType(rec.name);
        break;
        
      }
	    
      // notify the listeners
      updateRecord(now, rec);
    }
    //    print();
  }

  //For probing we need to save incoming probes that might have a conflict.
  synchronized void handleProbe(DNSIncoming msg) throws IOException
  {
    long now = System.currentTimeMillis();
    for (Enumeration e = msg.answers.elements() ; e.hasMoreElements() ;) {
      DNSRecord rec = (DNSRecord)e.nextElement();
      boolean expired = rec.isExpired(now);

      // update the cache
      DNSRecord c = (DNSRecord)queryCache.get(rec);
      if (c != null) {
        if (expired) {
          queryCache.remove(c);
        } else {
          c.resetTTL(rec);
          rec = c;
        }
      } else if (!expired) {
        queryCache.add(rec);
      }
      // notify the listeners
    }
  }

  /**
   * Add an answer to a question. Deal with the case when the
   * outgoing packet overflows
   */
  DNSOutgoing addAnswer(DNSIncoming in, InetAddress addr, int port, DNSOutgoing out, DNSRecord rec) throws IOException
  {
    if (out == null) {
      out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
    }
    try {
      out.addAnswer(in, rec);
    } catch (IOException e) {
      out.flags |= DNSConstants.FLAGS_TC;
      out.id = in.id;
      out.finish();
      socket.send(new DatagramPacket(out.data, out.off, addr, port));

      out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
      out.addAnswer(in, rec);
    }
    return out;
  }

  synchronized private DNSOutgoing  typeA( DNSQuestion q, 
                                           DNSIncoming in, 
                                           InetAddress addr, 
                                           int port, DNSOutgoing out ) 
    throws IOException
  {
    if( ( host != null ) && q.name.equalsIgnoreCase( host.name ) ) 
    {
      out = addAnswer( in, addr, port, out, host );
      return out;

    }
    return out;
  }

  private DNSOutgoing typePTR( DNSQuestion q, DNSIncoming in, 
                               InetAddress addr, int port, 
                               DNSOutgoing out, Vector additionals ) 
    throws IOException 
  {
    registerServiceType( q.name ); // Wait time needed for shared query response.
    waitInterval( DNSConstants.SHARED_QUERY_TIME );
    // find matching services
    for( Enumeration s = services.elements(); s.hasMoreElements(); ) 
    {
      ServiceInfo info = (ServiceInfo)s.nextElement();
      if( q.name.equalsIgnoreCase( info.type ) ) 
      {
        out = addAnswer( in, addr, port, out, 
                         new DNSRecord.Pointer( info.type, DNSConstants.TYPE_PTR, 
                                                DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, 
                                                info.name ) );
        additionals.addElement( host );
        additionals.addElement( new DNSRecord.Service( info.name, DNSConstants.TYPE_SRV, 
                                                       DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, 
                                                       DNSConstants.DNS_TTL, info.priority, 
                                                       info.weight, info.port, host.name ) );
        additionals.addElement( new DNSRecord.Text( info.name, DNSConstants.TYPE_TXT, 
                                                    DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, 
                                                    DNSConstants.DNS_TTL, info.text ) );
      }
    }
    if( q.name.equalsIgnoreCase( "_services._mdns._udp.local." ) ) 
    {
      for( Enumeration t = serviceTypes.elements(); t.hasMoreElements(); ) 
      {
        out = addAnswer( in, addr, port, out,
                         new DNSRecord.Pointer( "_services._mdns._udp.local.", 
                                                DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, 
                                                DNSConstants.DNS_TTL, (String)t.nextElement() ) );
      }
    }
    return out;
  }

  private DNSOutgoing findService( DNSQuestion q, DNSIncoming in, 
                                   InetAddress addr, int port, DNSOutgoing out, Vector additionals ) 
    throws IOException 
  {
    ServiceInfo info = (ServiceInfo)services.get( q.name.toLowerCase() );
    if( info != null ) 
    {
      if( ( q.type == DNSConstants.TYPE_SRV ) || ( q.type == DNSConstants.TYPE_ANY ) ) 
      {
        out = addAnswer( in, addr, port, out, 
                         new DNSRecord.Service( q.name, DNSConstants.TYPE_SRV,
                                                DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, 
                                                DNSConstants.DNS_TTL, info.priority, 
                                                info.weight, info.port, host.name ) );
      }
      if( ( q.type == DNSConstants.TYPE_TXT ) || ( q.type == DNSConstants.TYPE_ANY ) ) 
      {
        out = addAnswer( in, addr, port, out, 
                         new DNSRecord.Text(q.name, DNSConstants.TYPE_TXT, 
                                            DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE,
                                            DNSConstants.DNS_TTL, info.text ) );
      }
      additionals.addElement( host );
      
      additionals.addElement( new DNSRecord.Service( info.name, DNSConstants.TYPE_SRV, 
                                                     DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, 
                                                     DNSConstants.DNS_TTL, info.priority, 
                                                     info.weight, info.port, host.name ) );
      additionals.addElement( new DNSRecord.Text( info.name, DNSConstants.TYPE_TXT, 
                                                  DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, 
                                                  DNSConstants.DNS_TTL, info.text ) );
    }
    if( q.type == DNSConstants.TYPE_ANY ) 
    {
      
      //    System.err.println("Any Type: " + q);
      // Also check for Address Records.
      if( ( host != null ) && q.name.equalsIgnoreCase( host.name ) ) 
      {
        out = addAnswer(in, addr, port, out, host);
      }
      else    
        //lets  look at services
        for( Enumeration tt = serviceTypes.elements(); tt.hasMoreElements(); ) {
          String tname = (String) tt.nextElement();
          if( tname.equalsIgnoreCase( q.name ) ) {
            waitInterval( DNSConstants.SHARED_QUERY_TIME );    
            out = addAnswer( in, addr, port, out, 
                             new DNSRecord.Pointer( q.name, DNSConstants.TYPE_PTR, 
                                                    DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, tname ) );
          }
        }    
    }
    return out;
  }

  /**
   * Handle an incoming query. See if we can answer any part of it
   * given our registered records.
   */
  // reduced size of synchronized block to just the Socket.send() call
  // synchronized void handleQuery(DNSIncoming in, InetAddress addr, int port) throws IOException {
  void handleQuery(DNSIncoming in, InetAddress addr, int port) throws IOException 
  {
    DNSOutgoing out = null;
    Vector additionals = new Vector(5);  //may not be effiecient I know.
    // for unicast responses the question must be included
    if( port != DNSConstants.MDNS_PORT ) 
    {
      out = new DNSOutgoing( DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA, false );
      for ( Enumeration e = in.questions.elements(); e.hasMoreElements(); ) 
      {
        out.addQuestion( (DNSQuestion)e.nextElement() );
      }
    }
    // answer relevant questions
    for( Enumeration e = in.questions.elements(); e.hasMoreElements(); )
    {
      DNSQuestion q = (DNSQuestion)e.nextElement();
      switch( q.type ) 
      {
      case DNSConstants.TYPE_A: // address request
        out = typeA( q, in, addr, port, out );
        break;
      case DNSConstants.TYPE_PTR: // Domain Name request
        out = typePTR( q, in, addr, port, out, additionals );
        break;
      default: // find service
        out = findService( q, in, addr, port, out, additionals );
      } 
    }
    if( ( out != null ) && ( out.numAnswers > 0 ) )
    {  // add additional answers if there is space
      //      if (additionals != null) 
      if(additionals.size() > 0)
      
        for( Enumeration e = additionals.elements(); e.hasMoreElements(); ) 
        {
          out.addAdditionalAnswer( in, (DNSRecord)e.nextElement() );
        }
      
      out.id = in.id;
      out.finish();
      synchronized( this ) 
      {  // is it really this we want to synchronize on, or the socket??
        socket.send(new DatagramPacket( out.data, out.off, addr, port ) );
      }
    }
  }


  /**
   * Send an outgoing multicast DNS message.
   */
  synchronized void send(DNSOutgoing out) throws IOException
  {
    out.finish();
    socket.send(new DatagramPacket(out.data, out.off, group, DNSConstants.MDNS_PORT));
  }
 
  /**
   * Listen for multicast packets.
   */
  class SocketListener implements Runnable
  {
    private boolean inProbe = false;
    
    //Setup state so we can check for duplicate probes. 
    synchronized public void setProbe(boolean probing)
    {
      inProbe = probing;
    }
    public void run()
    {
      try {
        byte buf[] = new byte[DNSConstants.MAX_MSG_ABSOLUTE];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while (!done) {
          packet.setLength(buf.length);
          socket.receive(packet);
          if (done) {
            break;
          }
          try {
            InetAddress from = packet.getAddress();
            if (linklocal != isLinkLocal(from)) {
              // Ignore linklocal packets on regular interfaces, unless this is
              // also a linklocal interface. This is to avoid duplicates. This is
              // a terrible hack caused by the lack of an API to get the address
              // of the interface on which the packet was received.
              continue;
            }
            if (loopback != isLoopback(from)) {
              // Ignore loopback packets on a regular interface unless this is
              // also a loopback interface.
              continue;
            }
            DNSIncoming msg = new DNSIncoming(packet);
            if (debug > 0) {
              msg.print(debug > 1);
              System.out.println();
            }


            if (msg.isQuery()) 
            {
              handleProbe(msg);
              if (packet.getPort() != DNSConstants.MDNS_PORT) 
              {
                handleQuery(msg, packet.getAddress(), packet.getPort());
              }
              handleQuery(msg, group, DNSConstants.MDNS_PORT);
            } 
            else 
            {
              //We need to check it is a response /announcement to our address!

              handleResponse(msg);    
            }
            if(conflict(msg))
            {
              new Probe(true).start();
            }
          } 
          catch (IOException e) 
          {
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
            for (Iterator i = cache.all() ; i.hasNext() ; ) 
            {
              DNSRecord c = (DNSRecord)i.next();
              if (c.isExpired(now)) 
              {
                updateRecord(now, c);
                i.remove();
              }
            }
            // remove expired queries
            now = System.currentTimeMillis();
            for (Iterator i = queryCache.all() ; i.hasNext() ; ) 
            {
              DNSRecord c = (DNSRecord)i.next();
              if (c.isExpired(now)) 
              {
                updateRecord(now, c);
                i.remove();
              }
            }
          }
        }
      }
      catch (InterruptedException e) 
      {
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

      addListener(this, new DNSQuestion(type, DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN));
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
      if ((rec.type == DNSConstants.TYPE_PTR) && rec.name.equals(type)) {
        boolean expired = rec.isExpired(now);
        String name = ((DNSRecord.Pointer)rec).alias;
        DNSRecord old = (DNSRecord)services.get(name.toLowerCase());
        if ((old == null) && !expired) {
          // new record
          services.put(name.toLowerCase(), rec);
          list.addLast(new Event(name) {
              void send() {listener.addService(JmDNS.this, type, this.name);}
            });
        } else if ((old != null) && !expired) {
          // update record
          old.resetTTL(rec);
        } else if ((old != null) && expired) {
          // expire record
          services.remove(name.toLowerCase());
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
              DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
              out.addQuestion(new DNSQuestion(type, DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN));
              for (Enumeration e = services.elements() ; e.hasMoreElements() ;) {
                DNSRecord rec = (DNSRecord)e.nextElement();
                if (!rec.isExpired(now)) {
                  try {
                    out.addAnswer(rec, now);
                  } catch (IOException ee) {
                    break;
                  }
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
   * Helper class to resolve services.
   */
  class ServiceResolver implements Runnable
  {
    ServiceInfo info;
    int timeout;
	
    ServiceResolver(ServiceInfo info, int timeout)
    {
      this.info = info;
      this.timeout = timeout;
    }
    public void run()
    {
      ServiceInfo result = info;
      if (!info.request(JmDNS.this, timeout)) {
        result = null;
      }
      // notify the listeners of the appropriate service browsers
      for (Enumeration e = browsers.elements() ; e.hasMoreElements() ;) {
        ServiceBrowser browser = (ServiceBrowser)e.nextElement();
        if (browser.type.equalsIgnoreCase(info.type)) {
          browser.listener.resolveService(JmDNS.this, info.type, info.name, result);
        }
      }
    }
  }

  /**
   * Class to handle probing and initial announcement
   * This is a thread class since it must be able to be run async from the listeners
   * When a conflict is detected.  We should also do Service probing and Conflict
   * resolution here, but for now!!!
   **/

  class Probe extends Thread
  {
    boolean inConflict = false;
    public Probe()
    {
      super("Prober");

    }

    public Probe(boolean conflict)
    {
      super("Prober");
      inConflict = true;  // what a kludge. Needed to defend initial address mapping.
    }

    //One shot.  Dont want to stick around 
    public void run()
    {
      if(done)
        return;
      this.probe(host);
      this.announce(host);
    }

    //For tie breaker.  section 9.2  must compare unsigned bytes.  
    private byte[] getAddress(int addr)
    {
      byte[] rval = new byte[4];
      rval[0] = (byte)((addr >>> 24) & 0xFF);
      rval[1] = (byte)((addr >>> 16) & 0xFF);
      rval[2] = (byte)((addr >>> 8) & 0xFF); 
      rval[3] = (byte)(addr & 0xFF);
      return rval;
    }

    //return 1 if in1 > in2
    //return 0 if in1==in2
    //return -1 if in1>in2

    private int lexCompare(int in1, int in2)
    {
    
      if(in1 == in2)
        return 0;
      byte[] in1Bytes = getAddress(in1);
      byte[] in2Bytes = getAddress(in2);
      for(int i=0;i<in1Bytes.length;i++)
      {
        if (in1Bytes[i] > in2Bytes[i])
          return 1;
        else 
          if (in2Bytes[i] > in1Bytes[i])
            return -1;
      }
      return 0;  //Should never happen.
    }

    void probe(DNSRecord.Address inHost ) 
    {
      //Probe spec says initial wait time is a  random interval between 0 and 250 ms.
      int waitTime = new Random().nextInt(251);  

      boolean denial = false;
      boolean duplicate = false;
      DNSRecord  a;
      int conflictCount = 0;

      //Initial Run.  Set up the listeners. This is done here because we can have a 
      //local handle on the listener to set the probing state.        
      if (incomingListener == null)
      {
        incomingListener = new SocketListener();
        new Thread(incomingListener, "JmDNS.SocketListener").start();
      }
      incomingListener.setProbe(true);    

      // Wait random time
      waitInterval(waitTime);
      //Maybe we should always do send wait search instead of 
      // search send wait?  Should run the Conformance test again to find out.
      // initialy this failled when doing send first.

      //Defend initial choice if conflict detected.  Kindof a kludge.
      //but it makes the conformance test happy.
      //The host name will be found below and reset.
      if(inConflict)
      {
        try
        {
          DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
          //out.addQuestion(new DNSQuestion(inHost.name, TYPE_A, CLASS_IN));
          out.addQuestion(new DNSQuestion(inHost.name, DNSConstants.TYPE_ANY, DNSConstants.CLASS_IN));  
          out.addAuthorativeAnswer(inHost);
          send(out);
        }
        catch(java.io.IOException e)
        {
          e.printStackTrace();
        }
      }

      //Send 3 probes at 250ms interval. Per the spec.
      waitTime = DNSConstants.PROBE_WAIT_INTERVAL;
      long now = System.currentTimeMillis();
      long nextTime = now;
      for (int i = 0 ; i < 3 ;) 
      {

        Iterator j;
        //Set up initial states.
        denial = false;
        duplicate = false;


        //Look for a response first then look at incoming queries.
        for (j = cache.find(inHost.name) ; j.hasNext() ;) 
        {
          a = (DNSRecord)j.next();

          if   ((a.type == DNSConstants.TYPE_A) && !a.isExpired(now) && (((DNSRecord.Address)a).addr != _ip)) 
          {
            denial = true;
            break;
          }
        }

        for (j = queryCache.find(inHost.name) ; j.hasNext() ;) 
        {
          a = (DNSRecord)j.next();

          if   ((a.type == DNSConstants.TYPE_A) && !a.isExpired(now) && (((DNSRecord.Address)a).addr != _ip)) 
          {
            if(lexCompare(((DNSRecord.Address)a).addr, _ip) > 0)
            {
              duplicate = true;
              break;
            }
          }

        }
        if(denial || duplicate )
        {
          conflictCount++;

          //Throttle on conflict as per section 9.1  
          if (conflictCount > DNSConstants.PROBE_THROTTLE_COUNT)
          {
            conflictCount = 0;
            waitTime = DNSConstants.PROBE_CONFLICT_INTERVAL;
          }
        
          //this could be in its own method.  But only used here and only during init time.
          String nm = inHost.name.substring(0, inHost.name.indexOf(".local."));
          try 
          {
            int l = inHost.name.lastIndexOf('-'); 
            int r = inHost.name.lastIndexOf('.'); 

            if ((l >= 0) && (l < r)) 
            {
              nm = nm.substring(0, l) + "-" + (Integer.parseInt(nm.substring(l+1)) + 1);
            } 
            else 
            {
              nm += "-2";
            }   
          } 
          catch (NumberFormatException e) 
          {
            nm += "-2";
          }
          //This works because . is an illeagle charactor in local names.
          inHost.name = nm + inHost.name.substring(inHost.name.indexOf('.'));
          //Reset Loop count Going to try all over again.
          i=0;
        }
        i++;

        //Send the query.   
        try
        {

          DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
          //out.addQuestion(new DNSQuestion(inHost.name, TYPE_A, CLASS_IN));
          //Use Anytype as per the spec.
          out.addQuestion(new DNSQuestion(inHost.name, DNSConstants.TYPE_ANY, DNSConstants.CLASS_IN));  
          out.addAuthorativeAnswer(inHost);
          send(out);
        }
        catch(java.io.IOException e)
        {
          e.printStackTrace();
        }
        //Wait the amount of time
        nextTime+= waitTime;
        if (now < nextTime) 
        {
          //wait what is left of the probe wait time.
          waitInterval(nextTime - now); 
          now=System.currentTimeMillis();
        }

      }   
    
      host = inHost;
    }

    void announce(DNSRecord.Address host)
    {
      //As per section 9.3  Send a gratuitous response at least 2 times 1 sec apart.
      int waitTime = DNSConstants.ANNOUNCE_WAIT_INTERVAL;
      long now, nextTime;
      for(int i = 0;i<2;i++)
      {
        now = System.currentTimeMillis();        
        nextTime= now + waitTime;
        try
        {
          DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);    
          //Initial startup.  All we have is the host record.  Later add support
          //for starting with a list of services.  But for now......
          out.addAnswer(host, 0);     
          send(out);  
        }
        catch(java.io.IOException e)
        {
          //Should do something smarter here shuch as bomb out completly
          System.err.println("Announce got an error: " +e);
        }
        now = System.currentTimeMillis();      

        if (now < nextTime) 
        {
          //wait what is left of the probe wait time.
        
          waitInterval(nextTime - now);  
          continue;
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
  /**
   * List Services and serviceTypes.
   * Debugging Only
   **/

  public void printServices()
  {
    Enumeration k;
    System.err.println("---- Services -----");
    if(services != null)
    {
      k = services.keys();
      while(k.hasMoreElements())
      {
        String key = (String)k.nextElement();
        System.err.println("Service: " + key +": " + services.get(key));
      }
    }
    System.err.println("---- Types ----");
    if(serviceTypes!=null)
    {
      k = serviceTypes.keys();
      while(k.hasMoreElements())
      {
        String key = (String)k.nextElement();
        System.err.println("Type: " + key +": " + serviceTypes.get(key));
      }
    }
    System.err.println("");

  }  

}
