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
package javax.jmdns.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;
import org.slf4j.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class ITunesRemotePairing implements Runnable, ServiceListener {

    public final static String     TOUCH_ABLE_TYPE = "_touch-able._tcp.local.";
    public final static String     DACP_TYPE       = "_dacp._tcp.local.";
    public final static String     REMOTE_TYPE     = "_touch-remote._tcp.local.";

    public volatile static boolean _running        = true;
    protected final Random         random          = new Random();

    public static byte[]           PAIRING_RAW     = new byte[] { 0x63, 0x6d, 0x70, 0x61, 0x00, 0x00, 0x00, 0x3a, 0x63, 0x6d, 0x70, 0x67, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x63, 0x6d, 0x6e, 0x6d, 0x00, 0x00,
            0x00, 0x16, 0x41, 0x64, 0x6d, 0x69, 0x6e, 0x69, 0x73, 0x74, 0x72, 0x61, 0x74, 0x6f, 0x72, (byte) 0xe2, (byte) 0x80, (byte) 0x99, 0x73, 0x20, 0x69, 0x50, 0x6f, 0x64, 0x63, 0x6d, 0x74, 0x79, 0x00, 0x00, 0x00, 0x04, 0x69, 0x50, 0x6f, 0x64 };

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // Activate these lines to see log messages of JmDNS
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

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new ITunesRemotePairing());
        executor.shutdown();
    }

    /**
     *
     */
    public ITunesRemotePairing() {
        super();
    }

    @Override
    public void run() {
        try {
            final JmDNS jmdns = JmDNS.create();
            jmdns.addServiceListener(TOUCH_ABLE_TYPE, this);
            jmdns.addServiceListener(DACP_TYPE, this);

            final HashMap<String, String> values = new HashMap<String, String>();
            byte[] number = new byte[4];
            random.nextBytes(number);
            values.put("DvNm", "Android-" + toHex(number));
            values.put("RemV", "10000");
            values.put("DvTy", "iPod");
            values.put("RemN", "Remote");
            values.put("txtvers", "1");
            byte[] pair = new byte[8];
            random.nextBytes(pair);
            values.put("Pair", toHex(pair));

            while (_running) {
                ServerSocket server = new ServerSocket(0);

                byte[] name = new byte[20];
                random.nextBytes(name);
                System.out.println("Requesting pairing for " + toHex(name));
                ServiceInfo pairservice = ServiceInfo.create(REMOTE_TYPE, toHex(name), server.getLocalPort(), 0, 0, values);
                jmdns.registerService(pairservice);

                System.out.println("Waiting for pass code");
                final Socket socket = server.accept();
                OutputStream output = null;

                try {
                    output = socket.getOutputStream();

                    // output the contents for debugging
                    final BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    while (br.ready()) {
                        String line = br.readLine();
                        System.out.println(line);
                    }

                    // edit our local PAIRING_RAW to return the correct guid
                    byte[] code = new byte[8];
                    random.nextBytes(code);
                    System.out.println("Device guid: " + toHex(code));
                    System.arraycopy(code, 0, PAIRING_RAW, 16, 8);

                    byte[] header = String.format("HTTP/1.1 200 OK\r\nContent-Length: %d\r\n\r\n", new Integer(PAIRING_RAW.length)).getBytes();
                    byte[] reply = new byte[header.length + PAIRING_RAW.length];

                    System.arraycopy(header, 0, reply, 0, header.length);
                    System.arraycopy(PAIRING_RAW, 0, reply, header.length, PAIRING_RAW.length);

                    System.out.println("Response: " + new String(reply));

                    output.write(reply);
                    output.flush();

                    System.out.println("someone paired with me!");

                    jmdns.unregisterService(pairservice);
                } finally {
                    if (output != null) {
                        output.close();
                    }

                    System.out.println("Closing Socket");
                    if (!server.isClosed()) {
                        server.close();
                    }
                    _running = false;
                }
            }
            Thread.sleep(6000);
            System.out.println("Closing JmDNS");
            jmdns.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        System.out.println("Service added   : " + event.getName() + "." + event.getType());
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        System.out.println("Service removed : " + event.getName() + "." + event.getType());
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        System.out.println("Service resolved: " + event.getName() + "." + event.getType() + "\n" + event.getInfo());
    }

    private static final char[] _nibbleToHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

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