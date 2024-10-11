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
package javax.jmdns.impl;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author stefan eicher
 */
public class ServiceInfoImplTest {


    private JmDNSImpl jmDNS;

    @Test
    public void test_ip_address_is_set() throws Exception {
        byte[] buf = readFile("a_record_before_srv.bin");
        DNSIncoming msg = new DNSIncoming(new DatagramPacket(buf, buf.length));
        jmDNS = new JmDNSImpl(null, null);
        ServiceInfoImpl serviceInfo = new ServiceInfoImpl(
                "_ibisip_http._tcp.local.",
                "DeviceManagementService",
                "",
                -1,
                -1,
                -1,
                true,
                new byte[]{});
        jmDNS.addListener(serviceInfo, null);
        jmDNS.handleResponse(msg);


        //Assure init values are overwritten and that ..
        assertEquals(serviceInfo.getServer(), "DIST500_7-F07_OC030_05_03941.local.");
        assertEquals(serviceInfo.getPort(), 5000);
        assertEquals(serviceInfo.getWeight(), 0);
        assertEquals(serviceInfo.getPriority(), 0);
        assertEquals(serviceInfo.isPersistent(), true);

        // ... the ip address is set
        assertEquals(serviceInfo.getInet4Addresses().length, 1);
        assertArrayEquals(serviceInfo.getInet4Addresses()[0].getAddress(), new  byte[]{(byte) 192, (byte) 168,88, (byte) 236});
    }

    @After
    public void after() {
        if (jmDNS != null) jmDNS.close();
    }

    private byte[] readFile(String fileName) throws IOException {
        File file = new File(this.getClass().getResource(fileName).getFile());

        FileInputStream fileInputStream;
        byte[] buf = new byte[(int) file.length()];

        //convert file into array of bytes
        fileInputStream = new FileInputStream(file);
        fileInputStream.read(buf);
        fileInputStream.close();
        return buf;
    }
}
