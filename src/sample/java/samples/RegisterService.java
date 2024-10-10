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
import java.util.HashMap;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;
import org.slf4j.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

/**
 * Sample Code for Service Registration using JmDNS.
 * <p>
 * To see what happens, launch the TTY browser of JmDNS using the following command:
 *
 * <pre>
 * java -jar lib/jmdns.jar -bs _http._tcp local.
 * </pre>
 *
 * Then run the main method of this class. When you press 'r' and enter, you should see the following output on the TTY browser:
 *
 * <pre>
 * ADD: service[foo._http._tcp.local.,192.168.2.5:1234,path=index.html]
 * </pre>
 *
 * Press 'r' and enter, you should see the following output on the TTY browser:
 *
 * <pre>
 * ADD: service[foo._http._tcp.local.,192.168.2.5:1234,path=index.html]
 * </pre>
 *
 * REMOVE: foo
 *
 * @author Werner Randelshofer
 */
public class RegisterService {

    public final static String REMOTE_TYPE = "_touch-remote._tcp.local.";

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        // Activate these lines to see log messages of JmDNS
        boolean log = true;
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

        try {
            System.out.println("Opening JmDNS...");
            JmDNS jmdns = JmDNS.create();
            System.out.println("Opened JmDNS!");
            Random random = new Random();
            int id = random.nextInt(100000);
            System.out.println("\nPress r and Enter, to register Itunes Remote service 'Android-'" + id);
            int b;
            while ((b = System.in.read()) != -1 && (char) b != 'r') {
                /* Stub */
            }

            final HashMap<String, String> values = new HashMap<String, String>();
            values.put("DvNm", "Android-" + id);
            values.put("RemV", "10000");
            values.put("DvTy", "iPod");
            values.put("RemN", "Remote");
            values.put("txtvers", "1");
            byte[] pair = new byte[8];
            random.nextBytes(pair);
            values.put("Pair", toHex(pair));

            byte[] name = new byte[20];
            random.nextBytes(name);
            System.out.println("Requesting pairing for " + toHex(name));
            ServiceInfo pairservice = ServiceInfo.create(REMOTE_TYPE, toHex(name), 1025, 0, 0, values);
            jmdns.registerService(pairservice);

            System.out.println("\nRegistered Service as " + pairservice);
            System.out.println("Press q and Enter, to quit");
            // int b;
            while ((b = System.in.read()) != -1 && (char) b != 'q') {
                /* Stub */
            }
            System.out.println("Closing JmDNS...");
            jmdns.unregisterService(pairservice);
            jmdns.unregisterAllServices();
            jmdns.close();
            System.out.println("Done!");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final char[] _nibbleToHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static String toHex(byte[] code) {
        StringBuilder result = new StringBuilder(2 * code.length);

        for (int i = 0; i < code.length; i++) {
            int b = code[i] & 0xFF;
            result.append(_nibbleToHex[b / 16]);
            result.append(_nibbleToHex[b % 16]);
        }

        return result.toString();
    }
}
