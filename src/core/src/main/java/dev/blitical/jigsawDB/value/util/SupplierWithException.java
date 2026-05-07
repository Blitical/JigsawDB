package dev.blitical.jigsawDB.value.util;

@FunctionalInterface
public interface SupplierWithException<T> {
    T get() throws Exception;
}
