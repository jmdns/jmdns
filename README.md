![build status](https://travis-ci.org/jmdns/jmdns.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jmdns/jmdns/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jmdns/jmdns)
[![Javadocs](http://www.javadoc.io/badge/org.jmdns/jmdns.svg)](http://www.javadoc.io/doc/org.jmdns/jmdns)

This library is licensed under the Apache License Version 2.0.
Please see the file [NOTICE.txt](NOTICE.txt).  

Arthur van Hoff
avh@strangeberry.com

Rick Blair
rickblair@mac.com

Kai Kreuzer
kai@openhab.org

# JmDNS

This is an implemenation of multi-cast DNS in Java. It
supports service discovery and service registration. It is fully
interoperable with Apple's Bonjour. 

## Sample Code for Service Registration

```java
import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class ExampleServiceRegistration {

    public static void main(String[] args) throws InterruptedException {

        try {
            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            // Register a service
            ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", "example", 1234, "path=index.html");
            jmdns.registerService(serviceInfo);

            // Wait a bit
            Thread.sleep(25000);

            // Unregister all services
            jmdns.unregisterAllServices();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
```


## Sample code for Service Discovery

```java
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class ExampleServiceDiscovery {

    private static class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("Service added: " + event.getInfo());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            System.out.println("Service resolved: " + event.getInfo());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            // Add a service listener
            jmdns.addServiceListener("_http._tcp.local.", new SampleListener());

            // Wait a bit
            Thread.sleep(30000);
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
```
