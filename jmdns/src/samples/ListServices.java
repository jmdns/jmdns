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
 * Sample Code for Listing Services using JmDNS.
 * <p>
 * Run the main method of this class. This class prints a list of available HTTP
 * services every 5 seconds.
 *
 * @author  Werner Randelshofer
 * @version 	%I%, %G%
 */
public class ListServices {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Activate these lines to see log messages of JmDNS
        Logger logger = Logger.getLogger(JmDNS.class.toString());
        ConsoleHandler handler = new ConsoleHandler();
        logger.addHandler(handler);
        logger.setLevel(Level.FINER);
        handler.setLevel(Level.FINER);
        */
        
        try {
            JmDNS jmdns = new JmDNS();
            while (true) {
                ServiceInfo[] infos = jmdns.list("_http._tcp.local.");
                for (int i=0; i < infos.length; i++) {
                    System.out.println(infos[i]);
                }
                System.out.println();
                
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
