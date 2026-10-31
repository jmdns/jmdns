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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

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

        // A signal that arrives before the thread has started waiting in waitForEvent() is lost.
        // The thread may not have been scheduled yet, so we keep signaling until it reports that it
        // has been woken up. This avoids the fixed sleeps that make the test flaky on loaded runners.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!thread.hasFinished() && System.nanoTime() < deadline) {
            Thread.sleep(10);
            semaphore.signalEvent();
        }
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
