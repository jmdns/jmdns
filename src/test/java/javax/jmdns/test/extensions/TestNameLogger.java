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
package javax.jmdns.test.extensions;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestNameLogger implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private final Logger logger = LoggerFactory.getLogger(TestNameLogger.class);

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        logger.info("{}.{}() starting", extensionContext.getRequiredTestClass().getName(),
                extensionContext.getRequiredTestMethod().getName());
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) {
        logger.info("{}.{}() finished", extensionContext.getRequiredTestClass().getName(),
            extensionContext.getRequiredTestMethod().getName());
    }
}
