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
// Copyright 2003-2012 Arthur van Hoff, Rick Blair

package javax.jmdns.impl.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Custom thread factory which sets the name to make it easier to identify where the pooled threads were created.
 *
 * @author Trejkaz, Pierre Frisch
 */
public class NamedThreadFactory implements ThreadFactory {
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
