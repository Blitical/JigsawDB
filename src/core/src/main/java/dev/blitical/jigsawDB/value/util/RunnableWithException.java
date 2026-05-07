package dev.blitical.jigsawDB.value.util;

@FunctionalInterface
public interface RunnableWithException {
    void run() throws Exception;
}
