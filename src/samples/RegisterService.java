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

import javax.jmdns.*;
import java.io.IOException;
import java.util.logging.*;

/**
 * Sample Code for Service Registration using JmDNS.
 * <p>
 * To see what happens, launch the TTY browser of JmDNS using the following
 * command:
 * <pre>
 * java -jar lib/jmdns.jar -bs _http._tcp local.
 * </pre>
 * Then run the main method of this class. When you press 'r' and enter, 
 * you should see the following output on the TTY browser:
 * <pre>
 * ADD: service[foo._http._tcp.local.,192.168.2.5:1234,path=index.html]
 * </pre>
 *
 * Press 'r' and enter, 
 * you should see the following output on the TTY browser:
 * <pre>
 * ADD: service[foo._http._tcp.local.,192.168.2.5:1234,path=index.html]
 * </pre>
 * REMOVE: foo
 *
 * @author  Werner Randelshofer
 * @version 	%I%, %G%
 */
public class RegisterService {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Activate these lines to see log messages of JmDNS
        Logger logger = Logger.getLogger(JmDNS.class.getName());
        ConsoleHandler handler = new ConsoleHandler();
        logger.addHandler(handler);
        logger.setLevel(Level.FINER);
        handler.setLevel(Level.FINER);
        */
        
        try {
            System.out.println("Opening JmDNS");
            JmDNS jmdns = new JmDNS();
            System.out.println("Opened JmDNS");
            System.out.println("\nPress r and Enter, to register HTML service 'foo'");
            int b;
            while ((b = System.in.read()) != -1 && (char) b != 'r');
            ServiceInfo info = new ServiceInfo("_http._tcp.local.", "foo", 1268, 0, 0, "path=index.html");
            jmdns.registerService(info);
            
            System.out.println("\nRegistered Service as "+info);
            System.out.println("Press q and Enter, to quit");
            //int b;
            while ((b = System.in.read()) != -1 && (char) b != 'q'); 
            System.out.println("Closing JmDNS");
            jmdns.close();
            System.out.println("Done");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
