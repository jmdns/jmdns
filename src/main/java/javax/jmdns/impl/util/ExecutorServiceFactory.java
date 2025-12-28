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
package javax.jmdns.impl.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Factory class for creating customized {@link ExecutorService} instances with named threads.
 * This class provides methods to create single-thread, cached, and fixed-thread pool executors,
 * all of which utilize a {@link NamedThreadFactory} for thread naming.
 *
 * <p>Each method returns a new instance of an {@link ExecutorService} that is backed by a specific
 * thread pool implementation.
 */
public class ExecutorServiceFactory {

    private ExecutorServiceFactory() {
        // prevent instantiation in all static class
    }

    /**
     * Creates a new single-threaded executor service where all tasks are executed sequentially
     * in a single thread.
     *
     * @param name the name to be used for the thread in this executor
     * @return a newly created single-threaded {@link ExecutorService}
     */
    public static ExecutorService newSingleThreadExecutor(String name) {
        return Executors.newSingleThreadExecutor(new NamedThreadFactory(name));
    }

    /**
     * Creates a new cached thread pool executor service, where threads are created as needed
     * and reused when available.
     *
     * @param name the name to be used for the threads in this executor
     * @return a newly created cached thread pool {@link ExecutorService}
     */
    public static ExecutorService newCachedThreadPool(String name) {
        return Executors.newCachedThreadPool(new NamedThreadFactory(name));
    }

    /**
     * Creates a new fixed-size thread pool executor service, where a set number of threads are
     * created to handle tasks.
     *
     * @param size the number of threads in the pool
     * @param name the name to be used for the threads in this executor
     * @return a newly created fixed-size thread pool {@link ExecutorService}
     */
    public static ExecutorService newFixedThreadPool(int size, String name) {
        return Executors.newFixedThreadPool(size, new NamedThreadFactory(name));
    }

    /**
     * Custom thread factory which sets the name to make it easier to identify where the pooled threads were created.
     *
     * @author Trejkaz, Pierre Frisch
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadFactory _delegate;
        private final String _namePrefix;

        /**
         * Constructs the thread factory.
         *
         * @param namePrefix a prefix to append to thread names (will be separated from the default thread name by a space.)
         */
        public NamedThreadFactory(String namePrefix) {
            this._namePrefix = namePrefix;
            _delegate = Executors.defaultThreadFactory();
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = _delegate.newThread(runnable);
            thread.setName(_namePrefix + ' ' + thread.getName());
            return thread;
        }
    }
}