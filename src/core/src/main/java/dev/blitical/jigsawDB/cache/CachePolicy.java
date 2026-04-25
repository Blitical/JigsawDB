package dev.blitical.jigsawDB.cache;

import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.table.Table;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.time.Duration;
import java.util.*;

public class CachePolicy<S extends CachePolicy<S>> {
    protected enum Selector {
        ALL, NONE, CUSTOM, MAPPED
    }

    public enum Policy {
        EAGER, LAZY, NONE;
    }

    public static class PolicyMap<T extends Table<T, ?>> extends CachePolicy<PolicyMap<T>> {
        public final Set<Field<T, ?>> fields;

        @SafeVarargs
        public PolicyMap(Field<T, ?> field, Field<T, ?>... fields) {
            super(Selector.ALL);
            Set<Field<T, ?>> allFields = new HashSet<>();
            allFields.add(field);
            allFields.addAll(List.of(fields));
            this.fields = Set.copyOf(allFields);
        }

        public PolicyMap<T> cacheTheseFields(boolean cache) {
            this.selector = cache ? Selector.ALL : Selector.NONE;
            return this;
        }
    }

    public static class CustomCachePolicy<T extends Table<T, ?>> extends CachePolicy<CustomCachePolicy<T>> {
        protected final Set<Field<T, ?>> fields;

        public CustomCachePolicy(Set<Field<T, ?>> fields) {
            super(Selector.CUSTOM);
            this.fields = fields;
        }
    }

    public static class MappedCachePolicy<T extends Table<T, ?>> extends CachePolicy<MappedCachePolicy<T>> {
        protected final Map<Field<T, ?>, PolicyMap<T>> map;

        protected MappedCachePolicy(Map<Field<T, ?>, PolicyMap<T>> map) {
            super(Selector.MAPPED);
            this.map = map;
        }
    }

    public static class StaticCachePolicy extends CachePolicy<StaticCachePolicy> {
        public StaticCachePolicy(Selector policy) {
            super(policy);
        }
    }

    @Contract(value = " -> new", pure = true)
    public static <T extends Table<T, ?>> @NotNull StaticCachePolicy ALL() {
        return new StaticCachePolicy(Selector.ALL);
    }

    @Contract(value = " -> new", pure = true)
    public static <T extends Table<T, ?>> @NotNull StaticCachePolicy NONE() {
        return new StaticCachePolicy(Selector.NONE);
    }

    @Contract("_, _ -> new")
    @SafeVarargs
    public static <T extends Table<T, ?>> @NotNull CustomCachePolicy<T> of(
            Field<T, ?> field, Field<T, ?>... fields
    ) {
        Set<Field<T, ?>> allFields = new HashSet<>();
        allFields.add(field);
        allFields.addAll(List.of(fields));

        return new CustomCachePolicy<>(Set.copyOf(allFields));
    }

    @Contract("_ -> new")
    public static <T extends Table<T, ?>> @NotNull MappedCachePolicy<T> of(
            Set<PolicyMap<T>> set
    ) {
        Map<Field<T, ?>, PolicyMap<T>> map = new HashMap<>();

        set.forEach(m -> {
            m.fields.forEach(f -> {
                map.put(f, m);
            });
        });

        return new MappedCachePolicy<>(map);
    }

    @Contract("_, _ -> new")
    @SafeVarargs
    public static <T extends Table<T, ?>> @NotNull MappedCachePolicy<T> of(
            PolicyMap<T> val, PolicyMap<T>... vals
    ) {
        Set<PolicyMap<T>> set = new HashSet<>();
        set.add(val);
        set.addAll(List.of(vals));
        return of(set);
    }


    protected Selector selector;
    // -2 = Default, -1 = No Maximum
    // We need to know if its Default to determine if we
    // should recurse to find parent's max calls value
    protected int maxCalls = -2;
    // null = default
    protected Duration duration = null;
    // null = default. Default is Policy.EAGER
    protected Policy policy = null;

    private CachePolicy(Selector selector) {
        this.selector = selector;
    }

    @SuppressWarnings("unchecked")
    private S self() {
        return (S) this;
    }

    public S maxCalls(@Range(from = 1, to = Integer.MAX_VALUE) int maxCalls) {
        this.maxCalls = maxCalls;
        return self();
    }

    public S maxCallsInfinite() {
        this.maxCalls = -1;
        return self();
    }

    public S expireAfter(Duration duration) {
        this.duration = duration;
        return self();
    }

    public S withCachePolicy(@NotNull Policy policy) {
        this.policy = policy;
        return self();
    }

    public S duplicate(CachePolicy<?> policy) {
        this.maxCalls = policy.maxCalls;
        this.policy = policy.policy;
        return self();
    }
}
