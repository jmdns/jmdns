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
package javax.jmdns.test;

import static org.junit.Assert.*;

import javax.jmdns.impl.DNSStatefulObject.DNSStatefulObjectSemaphore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DNSStatefulObjectTest {

    public static final class WaitingThread extends Thread {

        private final DNSStatefulObjectSemaphore _semaphore;
        private final long                       _timeout;

        private boolean _hasFinished;

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
