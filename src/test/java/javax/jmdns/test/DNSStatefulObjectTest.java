package javax.jmdns.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jmdns.impl.DNSStatefulObject.DNSStatefulObjectSemaphore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DNSStatefulObjectTest {

    private static final class WaitingThread extends Thread {

        private final DNSStatefulObjectSemaphore semaphore;
        private final long timeout;

        private boolean hasFinished;

        public WaitingThread(DNSStatefulObjectSemaphore semaphore, long timeout) {
            super("Waiting thread");
            this.semaphore = semaphore;
            this.timeout = timeout;
            hasFinished = false;
        }

        @Override
        public void run() {
            semaphore.waitForEvent(timeout);
            hasFinished = true;
        }

        /**
         * @return the hasFinished
         */
        public boolean hasFinished() {
            return hasFinished;
        }

    }

    DNSStatefulObjectSemaphore semaphore;

    @BeforeEach
    public void setup() {
        semaphore = new DNSStatefulObjectSemaphore("test");
    }

    @AfterEach
    public void teardown() {
        semaphore = null;
    }

    @Test
    void testWaitAndSignal() throws InterruptedException {
        WaitingThread thread = new WaitingThread(semaphore, Long.MAX_VALUE);
        thread.start();
        Thread.sleep(1);
        assertFalse(thread.hasFinished(), "The thread should be waiting.");
        semaphore.signalEvent();
        Thread.sleep(1);
        assertTrue(thread.hasFinished(), "The thread should have finished.");
    }

    @Test
    void testWaitAndTimeout() throws InterruptedException {
        WaitingThread thread = new WaitingThread(semaphore, 100);
        thread.start();
        Thread.sleep(1);
        assertFalse(thread.hasFinished(), "The thread should be waiting.");
        Thread.sleep(150);
        assertTrue(thread.hasFinished(), "The thread should have finished.");
    }

}
