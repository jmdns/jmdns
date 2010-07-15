//Licensed under Apache License version 2.0
//Original license LGPL

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

package com.strangeberry.jmdns.tools;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

/**
 * Main sample program for JmDNS.
 *
 * @author Arthur van Hoff, Werner Randelshofer
 * @version %I%, %G%
 */
public class Main
{
    static class SampleListener implements ServiceListener, ServiceTypeListener
    {
        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.ServiceListener#serviceAdded(javax.jmdns.ServiceEvent)
         */
        public void serviceAdded(ServiceEvent event)
        {
            System.out.println("ADD: " + event.getDNS().getServiceInfo(event.getType(), event.getName()));
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.ServiceListener#serviceRemoved(javax.jmdns.ServiceEvent)
         */
        public void serviceRemoved(ServiceEvent event)
        {
            System.out.println("REMOVE: " + event.getName());
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.ServiceListener#serviceResolved(javax.jmdns.ServiceEvent)
         */
        public void serviceResolved(ServiceEvent event)
        {
            System.out.println("RESOLVED: " + event.getInfo());
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.ServiceTypeListener#serviceTypeAdded(javax.jmdns.ServiceEvent)
         */
        public void serviceTypeAdded(ServiceEvent event)
        {
            System.out.println("TYPE: " + event.getType());
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.ServiceTypeListener#subTypeForServiceTypeAdded(javax.jmdns.ServiceEvent)
         */
        @Override
        public void subTypeForServiceTypeAdded(ServiceEvent event)
        {
            System.out.println("SUBTYPE: " + event.getType());
        }
    }

    public static void main(String argv[]) throws IOException
    {
        int argc = argv.length;
        boolean debug = false;
        InetAddress intf = null;

        if ((argc > 0) && "-d".equals(argv[0]))
        {
            System.arraycopy(argv, 1, argv, 0, --argc);

            {
                ConsoleHandler handler = new ConsoleHandler();
                handler.setLevel(Level.FINEST);
                for (Enumeration<String> enumerator = LogManager.getLogManager().getLoggerNames(); enumerator.hasMoreElements();)
                {
                    String loggerName = enumerator.nextElement();
                    Logger logger = Logger.getLogger(loggerName);
                    logger.addHandler(handler);
                    logger.setLevel(Level.FINEST);
                }
            }
            debug = true;
        }

        if ((argc > 1) && "-i".equals(argv[0]))
        {
            intf = InetAddress.getByName(argv[1]);
            System.arraycopy(argv, 2, argv, 0, argc -= 2);
        }
        if (intf == null)
        {
            intf = InetAddress.getLocalHost();
        }

        JmDNS jmdns = JmDNS.create(intf, "Browser");

        if ((argc == 0) || ((argc >= 1) && "-browse".equals(argv[0])))
        {
            new Browser(jmdns);
            for (int i = 2; i < argc; i++)
            {
                jmdns.registerServiceType(argv[i]);
            }
        }
        else if ((argc == 1) && "-bt".equals(argv[0]))
        {
            jmdns.addServiceTypeListener(new SampleListener());
        }
        else if ((argc == 3) && "-bs".equals(argv[0]))
        {
            jmdns.addServiceListener(argv[1] + "." + argv[2], new SampleListener());
        }
        else if ((argc > 4) && "-rs".equals(argv[0]))
        {
            String type = argv[2] + "." + argv[3];
            String name = argv[1];
            Hashtable<String, Object> props = null;
            for (int i = 5; i < argc; i++)
            {
                int j = argv[i].indexOf('=');
                if (j < 0)
                {
                    throw new RuntimeException("not key=val: " + argv[i]);
                }
                if (props == null)
                {
                    props = new Hashtable<String, Object>();
                }
                props.put(argv[i].substring(0, j), argv[i].substring(j + 1));
            }
            jmdns.registerService(ServiceInfo.create(type, name, Integer.parseInt(argv[4]), 0, 0, props));

            // This while loop keeps the main thread alive
            while (true)
            {
                try
                {
                    Thread.sleep(Integer.MAX_VALUE);
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }
        else if ((argc == 2) && "-f".equals(argv[0]))
        {
            new Responder(jmdns, argv[1]);
        }
        else if (!debug)
        {
            System.out.println();
            System.out.println("jmdns:");
            System.out.println("     -d                                       - output debugging info");
            System.out.println("     -i <addr>                                - specify the interface address");
            System.out.println("     -browse [<type>...]                      - GUI browser (default)");
            System.out.println("     -bt                                      - browse service types");
            System.out.println("     -bs <type> <domain>                      - browse services by type");
            System.out.println("     -rs <name> <type> <domain> <port> <txt>  - register service");
            System.out.println("     -f <file>                                - rendezvous responder");
            System.out.println();
            System.exit(1);
        }
    }
}
