package javax.jmdns.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author stefan eicher
 */
class ServiceInfoImplTest {

    @BeforeEach
    public void before() throws IOException {
        jmDNS = new JmDNSImpl(null, null);
    }

    @AfterEach
    public void after() {
        if (jmDNS != null) jmDNS.close();
    }

    private JmDNSImpl jmDNS;

    @Test
    void testGetInet4Addresses() throws Exception {
        byte[] buf = readFile("a_record_before_srv.bin");
        DNSIncoming msg = new DNSIncoming(new DatagramPacket(buf, buf.length));
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

        //Assure init values are overwritten and that
        assertEquals("DIST500_7-F07_OC030_05_03941.local.", serviceInfo.getServer());
        assertEquals(5000, serviceInfo.getPort());
        assertEquals(0, serviceInfo.getWeight());
        assertEquals(0, serviceInfo.getPriority());
        assertTrue(serviceInfo.isPersistent());

        // ... the ip address is set
        assertEquals(1, serviceInfo.getInet4Addresses().length);
        assertArrayEquals(serviceInfo.getInet4Addresses()[0].getAddress(), new  byte[]{(byte) 192, (byte) 168,88, (byte) 236});
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
