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
 * Sample Code for Service Type Discovery using JmDNS and a ServiceTypeListener.
 * <p>
 * Run the main method of this class. It lists all service types known on the
 * local network on System.out.
 *
 * @author  Werner Randelshofer
 * @version 	%I%, %G%
 */
public class DiscoverServiceTypes {
    
    static class SampleListener implements ServiceTypeListener {
        
        public void serviceTypeAdded(ServiceEvent event) {
            System.out.println("Service type added: " +event.getType());
        }
    }
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
            jmdns.addServiceTypeListener(new SampleListener());
            
            System.out.println("Press q and Enter, to quit");
            int b;
            while ((b = System.in.read()) != -1 && (char) b != 'q');
            jmdns.close();
            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
