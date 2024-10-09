package javax.jmdns.test.extensions;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestNameLogger implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final Logger logger = LoggerFactory.getLogger(TestNameLogger.class);

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
