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

/**
 * JDK21 Implementation to convert all Executors to Virtual Threads.
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
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Creates a new cached thread pool executor service, where threads are created as needed
     * and reused when available.
     *
     * @param name the name to be used for the threads in this executor
     * @return a newly created cached thread pool {@link ExecutorService}
     */
    public static ExecutorService newCachedThreadPool(String name) {
        return Executors.newVirtualThreadPerTaskExecutor();
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
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}