/**
 *
 */
package javax.jmdns.test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import javax.jmdns.impl.DNSStatefulObject.DNSStatefulObjectSemaphore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class DNSStatefulObjectTest {

    public static final class WaitingThread extends Thread {

        private final DNSStatefulObjectSemaphore _semaphore;
        private final long                       _timeout;

        private boolean                          _hasFinished;

        public WaitingThread(DNSStatefulObjectSemaphore semaphore, long timeout) {
            super("Waiting thread");
            _semaphore = semaphore;
            _timeout = timeout;
            _hasFinished = false;
        }

        @Override
        public void run() {
            _semaphore.waitForEvent(_timeout);
            _hasFinished = true;
        }

        /**
         * @return the hasFinished
         */
        public boolean hasFinished() {
            return _hasFinished;
        }

    }

    DNSStatefulObjectSemaphore _semaphore;

    @Before
    public void setup() {
        _semaphore = new DNSStatefulObjectSemaphore("test");
    }

    @After
    public void teardown() {
        _semaphore = null;
    }

    @Test
    public void testWaitAndSignal() throws InterruptedException {
        WaitingThread thread = new WaitingThread(_semaphore, Long.MAX_VALUE);
        thread.start();
        Thread.sleep(1);
        assertFalse("The thread should be waiting.", thread.hasFinished());
        _semaphore.signalEvent();
        Thread.sleep(1);
        assertTrue("The thread should have finished.", thread.hasFinished());
    }

    @Test
    public void testWaitAndTimeout() throws InterruptedException {
        WaitingThread thread = new WaitingThread(_semaphore, 100);
        thread.start();
        Thread.sleep(1);
        assertFalse("The thread should be waiting.", thread.hasFinished());
        Thread.sleep(150);
        assertTrue("The thread should have finished.", thread.hasFinished());
    }

}
