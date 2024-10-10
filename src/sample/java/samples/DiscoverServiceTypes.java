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

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceTypeListener;

/**
 * Sample Code for Service Type Discovery using JmDNS and a ServiceTypeListener.
 * <p>
 * Run the main method of this class. It lists all service types known on the local network on System.out.
 *
 * @author Werner Randelshofer
 */
public class DiscoverServiceTypes {

    static class SampleListener implements ServiceTypeListener {

        @Override
        public void serviceTypeAdded(ServiceEvent event) {
            System.out.println("Service type added: " + event.getType());
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.ServiceTypeListener#subTypeForServiceTypeAdded(javax.jmdns.ServiceEvent)
         */
        @Override
        public void subTypeForServiceTypeAdded(ServiceEvent event) {
            System.out.println("SubType for service type added: " + event.getType());
        }
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        /*
         * Activate these lines to see log messages of JmDNS Logger logger = LoggerFactory.getLogger(JmDNS.class); ConsoleHandler handler = new ConsoleHandler(); logger.addHandler(handler); logger.setLevel(Level.FINER);
         * handler.setLevel(Level.FINER);
         */

        try {
            JmDNS jmdns = JmDNS.create();
            jmdns.addServiceTypeListener(new SampleListener());

            System.out.println("Press q and Enter, to quit");
            int b;
            while ((b = System.in.read()) != -1 && (char) b != 'q') {
                /* Stub */
            }
            jmdns.close();
            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
