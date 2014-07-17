/**
 *
 */
package javax.jmdns.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.DNSOutgoing.MessageFullException;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.tasks.resolver.ServiceResolver;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class DNSMessageTest {

    @Before
    public void setup() {
        boolean log = false;
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
    }

    private static final byte[] nmap_scan_package = new byte[] { 0x30, (byte) 0x82, 0x00, 0x2f, 0x02, 0x01, 0x00, 0x04, 0x06, 0x70, 0x75, 0x62, 0x6c, 0x69, 0x63, (byte) 0xa0, (byte) 0x82, 0x00, 0x20, 0x02, 0x04, 0x4c, 0x33, (byte) 0xa7, 0x56, 0x02,
            0x01, 0x00, 0x02, 0x01, 0x00, 0x30, (byte) 0x82, 0x00, 0x10, 0x30, (byte) 0x82, 0x00, 0x0c, 0x06, 0x08, 0x2b, 0x06, 0x01, 0x02, 0x01, 0x01, 0x05, 0x00, 0x05, 0x00 };

    @Test
    public void testIncomingOverflow() {
        try {
            // The DNSIncoming constructor should probably do bounds checking on the following parts of the package: questions, answers, authorities, additionals
            // The package above results in these values
            // questions -> 513
            // answers -> 4
            // authorities -> 1648
            // additionals -> 30050
            new DNSIncoming(new DatagramPacket(nmap_scan_package, nmap_scan_package.length, InetAddress.getByName(DNSConstants.MDNS_GROUP), DNSConstants.MDNS_PORT));
            fail("This message should have triggered an IO exception");
        } catch (IOException exception) {
            // All is OK
        }
    }
    
    private static final byte[] truncated_package = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x0b, 0x5f, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65, 0x63, 0x61, 0x73, 0x74, 0x04, 0x5f, 0x74, 0x63, 0x70, 0x05,
            0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x00, 0x00, 0x0c, 0x00, 0x01, 0x0b, 0x5f, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65, 0x63, 0x61, 0x73, 0x74, 0x04, 0x5f };
   
    @Test
    public void testTruncatedMessage() {
        try {
            new DNSIncoming(new DatagramPacket(truncated_package, truncated_package.length, InetAddress.getByName(DNSConstants.MDNS_GROUP), DNSConstants.MDNS_PORT));
            fail("This message should have triggered an IO exception");
        } catch (IOException exception) {
            // All is OK
        }
    }

    @Test
    public void testCreateQuery() throws IOException, MessageFullException {
        String serviceName = "_00000000-0b44-f234-48c8-071c565644b3._sub._home-sharing._tcp.local.";
        DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
        assertNotNull("Could not create the outgoing message", out);
        out.addQuestion(DNSQuestion.newQuestion(serviceName, DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, true));
        byte[] data = out.data();
        assertNotNull("Could not encode the outgoing message", data);
        byte[] expected = new byte[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x1, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x25, 0x5f, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x2d, 0x30, 0x62, 0x34, 0x34, 0x2d, 0x66, 0x32, 0x33, 0x34, 0x2d, 0x34, 0x38, 0x63, 0x38,
                0x2d, 0x30, 0x37, 0x31, 0x63, 0x35, 0x36, 0x35, 0x36, 0x34, 0x34, 0x62, 0x33, 0x4, 0x5f, 0x73, 0x75, 0x62, 0xd, 0x5f, 0x68, 0x6f, 0x6d, 0x65, 0x2d, 0x73, 0x68, 0x61, 0x72, 0x69, 0x6e, 0x67, 0x4, 0x5f, 0x74, 0x63, 0x70, 0x5, 0x6c,
                0x6f, 0x63, 0x61, 0x6c, 0x0, 0x0, (byte) 0xff, 0x0, 0x1 };
        for (int i = 0; i < data.length; i++) {
            assertEquals("the encoded message is not what is expected at index " + i, expected[i], data[i]);
        }
        DatagramPacket packet = new DatagramPacket(data, 0, data.length);
        DNSIncoming in = new DNSIncoming(packet);
        assertTrue("Wrong packet type.", in.isQuery());
        assertEquals("Wrong number of questions.", 1, in.getNumberOfQuestions());
        for (DNSQuestion question : in.getQuestions()) {
            assertEquals("Wrong question name.", serviceName, question.getName());
        }
    }

    @Test
    public void testCreateAnswer() throws IOException, MessageFullException {
        String serviceType = "_home-sharing._tcp.local.";
        String serviceName = "Pierre." + serviceType;
        DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA, false);
        assertNotNull("Could not create the outgoing message", out);
        out.addQuestion(DNSQuestion.newQuestion(serviceName, DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, true));
        long now = (new Date()).getTime();
        out.addAnswer(new DNSRecord.Pointer(serviceType, DNSRecordClass.CLASS_IN, true, DNSConstants.DNS_TTL, serviceName), now);
        out.addAuthorativeAnswer(new DNSRecord.Service(serviceType, DNSRecordClass.CLASS_IN, true, DNSConstants.DNS_TTL, 1, 20, 8080, "panoramix.local."));
        byte[] data = out.data();
        assertNotNull("Could not encode the outgoing message", data);
        // byte[] expected = new byte[] { 0x0, 0x0, (byte) 0x84, 0x0, 0x0, 0x1, 0x0, 0x1, 0x0, 0x1, 0x0, 0x0, 0x6, 0x50, 0x69, 0x65, 0x72, 0x72, 0x65, 0xd, 0x5f, 0x68, 0x6f, 0x6d, 0x65, 0x2d, 0x73, 0x68, 0x61, 0x72, 0x69, 0x6e, 0x67, 0x4, 0x5f, 0x74,
        // 0x63, 0x70, 0x5, 0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x0, 0x0, (byte) 0xff, 0x0, 0x1, (byte) 0xc0, 0x13, 0x0, 0xc, 0x0, 0x1, 0x0, 0x0, 0xe, 0xf, 0x0, 0x21, 0x6, 0x50, 0x69, 0x65, 0x72, 0x72, 0x65, 0xd, 0x5f, 0x68, 0x6f, 0x6d, 0x65, 0x2d,
        // 0x73, 0x68, 0x61, 0x72, 0x69, 0x6e, 0x67, 0x4, 0x5f, 0x74, 0x63, 0x70, 0x5, 0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x0, (byte) 0xc0, 0x13, 0x0, 0x21, 0x0, 0x1, 0x0, 0x0, 0xe, 0xf, 0x0, 0x17, 0x0, 0x1, 0x0, 0x14, 0x1f, (byte) 0x90, 0x9, 0x70,
        // 0x61, 0x6e, 0x6f, 0x72, 0x61, 0x6d, 0x69, 0x78, 0x5, 0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x0 };
        // for (int i = 0; i < data.length; i++)
        // {
        // assertEquals("the encoded message is not what is expected at index " + i, expected[i], data[i]);
        // }
        DatagramPacket packet = new DatagramPacket(data, 0, data.length);
        DNSIncoming in = new DNSIncoming(packet);
        assertTrue("Wrong packet type.", in.isResponse());
        assertEquals("Wrong number of questions.", 1, in.getNumberOfQuestions());
        assertEquals("Wrong number of answers.", 1, in.getNumberOfAnswers());
        assertEquals("Wrong number of authorities.", 1, in.getNumberOfAuthorities());
        for (DNSQuestion question : in.getQuestions()) {
            assertEquals("Wrong question name.", serviceName, question.getName());
        }
    }

    public static class TestServiceResolver extends ServiceResolver {
        
        public TestServiceResolver(JmDNSImpl jmDNSImpl, String type) {
            super(jmDNSImpl, type);
        }
        
        public DNSOutgoing buildMessage() throws IOException {
            DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
            out = this.addQuestions(out);
            out = this.addAnswers(out);
            return out;
        }
        
    }
    
    @Test
    public void testServiceResolverMessage() throws IOException {
        String serviceType = "_googlecast._tcp.local.";
        String serviceName = "Pierre";
        String qualifiedName = serviceName + "." + serviceType;
        int servicePort = 8888;
        
        JmDNSImpl registry = new JmDNSImpl(null,null);
        TestServiceResolver resolver = new TestServiceResolver(registry, serviceType);
        ServiceInfo info = ServiceInfo.create(serviceType, serviceName, servicePort, "");
        registry.getServices().put(qualifiedName, info);
        DNSOutgoing out = resolver.buildMessage();
        byte[] data = out.data();
        assertNotNull("Could not encode the outgoing message", data);
        registry.close();

        DatagramPacket packet = new DatagramPacket(data, 0, data.length);
        DNSIncoming in = new DNSIncoming(packet);
        assertTrue("Wrong packet type.", in.isQuery());
        assertEquals("Wrong number of questions.", 1, in.getNumberOfQuestions());
        assertEquals("Wrong number of answers.", 1, in.getNumberOfAnswers());
        for (DNSQuestion question : in.getQuestions()) {
            assertEquals("Wrong question name.", serviceType, question.getName());
        }
        for (DNSRecord answer : in.getAnswers()) {
            assertEquals("Wrong answer name.", serviceType, answer.getName());
        }
        
    }
    
    protected void print(byte[] data) {
        System.out.print("{");
        for (int i = 0; i < data.length; i++) {
            int value = data[i] & 0xFF;
            if (i > 0) {
                System.out.print(",");
            }
            System.out.print(" 0x");
            System.out.print(Integer.toHexString(value));
            if (i % 20 == 0) {
                System.out.print("\n");
            }
        }
        System.out.print("}\n");
    }
}
