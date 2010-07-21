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

package samples;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

/**
 * Sample Code for Service Discovery using JmDNS and a ServiceListener.
 * <p>
 * Run the main method of this class. It listens for HTTP services and lists all changes on System.out.
 *
 * @author Werner Randelshofer
 * @version %I%, %G%
 */
public class DiscoverServices
{

    static class SampleListener implements ServiceListener
    {
        @Override
        public void serviceAdded(ServiceEvent event)
        {
            System.out.println("Service added   : " + event.getName() + "." + event.getType());
        }

        @Override
        public void serviceRemoved(ServiceEvent event)
        {
            System.out.println("Service removed : " + event.getName() + "." + event.getType());
        }

        @Override
        public void serviceResolved(ServiceEvent event)
        {
            System.out.println("Service resolved: " + event.getInfo());
        }
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args)
    {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try
        {

            // Activate these lines to see log messages of JmDNS
            boolean log = false;
            if (log)
            {
                Logger logger = Logger.getLogger(JmDNS.class.getName());
                ConsoleHandler handler = new ConsoleHandler();
                logger.addHandler(handler);
                logger.setLevel(Level.FINER);
                handler.setLevel(Level.FINER);
            }

            final JmDNS jmdns = JmDNS.create();
            executor.submit(new Runnable() {
                @Override
                public void run()
                {
                    jmdns.addServiceListener("_http._tcp.local.", new SampleListener());
                }
            });

            System.out.println("Press q and Enter, to quit");
            int b;
            while ((b = System.in.read()) != -1 && (char) b != 'q')
            {
                /* Stub */
            }
            jmdns.close();
            System.out.println("Done");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
