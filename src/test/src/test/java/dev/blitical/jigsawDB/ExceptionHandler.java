//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.blitical.jigsawDB;

import dev.blitical.jigsawDB.config.JigsawDBLogger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.util.Optional;

public class ExceptionHandler implements TestExecutionExceptionHandler {
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        Optional<Class<?>> clazz = context.getTestClass();
        String clazzName = clazz.map(aClass -> (aClass).getSimpleName() + "#").orElse("");
        String testName = clazzName + context.getDisplayName();
        JigsawDBLogger.severe(throwable, "Failed Test: " + testName, new Object[0]);

        try {
            Tests.destroy();
        } catch (Exception _) {
        }

        throw throwable;
    }
}
