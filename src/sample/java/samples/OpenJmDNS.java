// Licensed under Apache License version 2.0
// Original license LGPL

//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package samples;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;

/**
 * Sample Code that opens JmDNS in debug mode.
 * <p>
 * Run the main method of this class.
 *
 * @author Werner Randelshofer
 */
public class OpenJmDNS {
    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        try {
            /* Activate these lines to see log messages of JmDNS */
            boolean log = true;
            if (log) {
                ConsoleHandler handler = new ConsoleHandler();
                handler.setLevel(Level.FINEST);
                for (Enumeration<String> enumerator = LogManager.getLogManager().getLoggerNames(); enumerator.hasMoreElements();) {
                    String loggerName = enumerator.nextElement();
                    Logger logger = Logger.getLogger(loggerName);
                    logger.addHandler(handler);
                    logger.setLevel(Level.FINEST);
                }
            }

            JmDNS jmdns = JmDNS.create();

            System.out.println("Press q and Enter, to quit");
            int b;
            while ((b = System.in.read()) != -1 && (char) b != 'q') {
                /* Stub */
            }
            jmdns.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
