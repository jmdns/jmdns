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

/**
 * Sample Code that opens JmDNS in debug mode.
 * <p>
 * Run the main method of this class. 
 *
 * @author  Werner Randelshofer
 * @version 	%I%, %G%
 */
public class OpenJmDNS {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            System.setProperty("jmdns.debug", "2");
            JmDNS jmdns = new JmDNS();
            
            System.out.println("Press q and Enter, to quit");
            int b;
            while ((b = System.in.read()) != -1 && (char) b != 'q'); 
            jmdns.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
