/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.util.logging.LogManager;
import org.slf4j.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

/**
 * Sample Code for Listing Services using JmDNS.
 * <p>
 * Run the main method of this class. This class prints a list of available HTTP services every 5 seconds.
 *
 * @author Werner Randelshofer
 */
public class ListServices {

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        /* Activate these lines to see log messages of JmDNS */
        boolean log = false;
        if (log) {
            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(Level.FINEST);
            for (Enumeration<String> enumerator = LogManager.getLogManager().getLoggerNames(); enumerator.hasMoreElements();) {
                String loggerName = enumerator.nextElement();
                Logger logger = LoggerFactory.getLogger(loggerName);
                logger.addHandler(handler);
                logger.setLevel(Level.FINEST);
            }
        }

        JmDNS jmdns = null;
        try {
            jmdns = JmDNS.create();
            while (true) {
                ServiceInfo[] infos = jmdns.list("_airport._tcp.local.");
                System.out.println("List _airport._tcp.local.");
                for (int i = 0; i < infos.length; i++) {
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
        } finally {
            if (jmdns != null) try {
                jmdns.close();
            } catch (IOException exception) {
                //
            }
        }
    }
}
