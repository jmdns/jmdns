// Copyright (C) 2002  Strangeberry Inc.
// @(#)README.txt, 1.3, 11/29/2002
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

Arthur van Hoff
avh@strangeberry.com

** JRendezvous

This is an implemenation of Rendezvous in Java. It currently
supports service discovery and service registration.



** Requirements

JRendezvous has been tested only on Windows using the JDK 1.3.1
and JDK 1.4.0. It has also been tried on Mac OS/10 using JDK1.3.1.



** Running JRendezvous from the Command Line

GUI browser:

  java -jar jrendezvous.jar -browse

TTY browser for a particular service type:

  java -jar jrendezvous.jar -bs _http._tcp local.

Register a service:

  java -jar jrendezvous.jar -rs foobar _http._tcp local. 1234 index.html

To print debugging output specify -d as the first argument.  



** Sample Code for Service Registration

    import com.strangeberry.rendezvous.*;

    Rendezvous rendezvous = new Rendezvous();
    rendezvous.registerService(
    	new ServiceInfo("_http._tcp.local.", "foo._http._tcp.local.", 1234, "index.html")
    );


** Sample code for Serivice Discovery

    import com.strangeberry.rendezvous.*;

    static class SampleListener implements ServiceListener
    {
	public void addService(Rendezvous rendezvous, String type, String name)
	{
	    System.out.println("ADD: " + rendezvous.getServiceInfo(type, name));
	}
	public void removeService(Rendezvous rendezvous, String type, String name)
	{
	    System.out.println("REMOVE: " + name);
	}
    }

    Rendezvous rendezvous = new Rendezvous();
    rendezvous.addServiceListener("_http._tcp.local.", new SampleListener());



** Changes since 9-10-2002

- Rendezvous.SocketListener: check done flag to avoid exception on close.

- Main: no arguments lauches the browser. Now you can double click on
  the jar file

- Switch from GPL to LGPL. Now you can use JRendezvous in your
  products without having to put them in the public domain.
