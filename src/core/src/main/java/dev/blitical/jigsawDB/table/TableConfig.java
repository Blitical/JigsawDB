package dev.blitical.jigsawDB.table;

import dev.blitical.jigsawDB.cache.CachePolicy;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.fields.*;
import dev.blitical.jigsawDB.table.columnConfigs.GenericColumnConfig;
import dev.blitical.jigsawDB.table.columnConfigs.NumberColumnConfig;
import dev.blitical.jigsawDB.table.columnConfigs.PrimaryGenericColumnConfig;
import dev.blitical.jigsawDB.table.columnConfigs.PrimaryIntegerColumnConfig;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class TableConfig<E extends Table<E, ?>> {
    private final Map<Field<E, ?>, ColumnConfig<?>> columns = new HashMap<>();
    private final Table<E, ?> table;

    private CachePolicy<?> cachePolicy = null;
    private Set<Field<E, ?>> autoCacheOnGet = null;
    private String name = null;

    public record Exposed<E extends Table<E, ?>>(
            Map<Field<E, ?>, ColumnConfig<?>> columns,
            Table<E, ?> table,
            CachePolicy<?> cachePolicy,
            Set<Field<E, ?>> autoCacheOnGet,
            String name
    ) {
    }

    protected Exposed<E> getExposed() {
        return new Exposed<>(columns, table, cachePolicy, autoCacheOnGet, name);
    }

    protected TableConfig(Table<E, ?> table) {
        this.table = table;
        modifyCachePolicy().build(null);
    }

    @SuppressWarnings("unchecked")
    public final <T> GenericColumnConfig<T> column(GenericField<E, T> field) {
        return (GenericColumnConfig<T>) columns.computeIfAbsent(
                field,
                _ -> new GenericColumnConfig<>()
        );
    }

    @SuppressWarnings("unchecked")
    public final <T> NumberColumnConfig<T> column(NumberField<E, T> field) {
        return (NumberColumnConfig<T>) columns.computeIfAbsent(
                field,
                _ -> new NumberColumnConfig<T>()
        );
    }

    @SuppressWarnings("unchecked")
    public final <T> PrimaryGenericColumnConfig<T> column(PrimaryGenericField<E, T> field) {
        return (PrimaryGenericColumnConfig<T>) columns.computeIfAbsent(
                field,
                _ -> new PrimaryGenericColumnConfig<>()
        );
    }

    @SuppressWarnings("unchecked")
    public final <T> PrimaryIntegerColumnConfig<T> column(PrimaryIntegerField<E, T> field) {
        return (PrimaryIntegerColumnConfig<T>) columns.computeIfAbsent(
                field,
                _ -> new PrimaryIntegerColumnConfig<>()
        );
    }

    public static final class CachePolicyConfig<T extends Table<T, ?>> {
        private final Map<Field<T, ?>, List<CachePolicy<?>>> mapStore = new HashMap<>();
        private final Table<T, ?> table;
        private final Consumer<CachePolicy.MappedCachePolicy<T>> consumer;

        public CachePolicyConfig(
                Table<T, ?> table,
                Consumer<CachePolicy.MappedCachePolicy<T>> consumer
        ) {
            this.table = table;
            this.consumer = consumer;
        }

        public CachePolicyConfig<T> field(
                Field<T, ?> field,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            mapStore.computeIfAbsent(field, _ -> new ArrayList<>())
                    .add(policy.apply(new CachePolicy.CustomCachePolicy<>(Set.of(field))));
            build(null);
            return this;
        }

        public CachePolicyConfig<T> field(
                Set<Field<T, ?>> fields,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            fields.forEach(f -> field(f, policy));
            return this;
        }

        @SafeVarargs
        public final CachePolicyConfig<T> field(
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy,
                Field<T, ?>... fields
        ) {
            for (var f : fields)
                field(f, policy);
            return this;
        }

        public CachePolicyConfig<T> field(
                Field<T, ?> f1, Field<T, ?> f2,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            return field(Set.of(f1, f2), policy);
        }

        public CachePolicyConfig<T> field(
                Field<T, ?> f1, Field<T, ?> f2, Field<T, ?> f3,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            return field(Set.of(f1, f2, f3), policy);
        }

        public CachePolicyConfig<T> field(
                Field<T, ?> f1, Field<T, ?> f2, Field<T, ?> f3, Field<T, ?> f4,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            return field(Set.of(f1, f2, f3, f4), policy);
        }

        public CachePolicyConfig<T> field(
                Field<T, ?> f1, Field<T, ?> f2, Field<T, ?> f3, Field<T, ?> f4, Field<T, ?> f5,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            return field(Set.of(f1, f2, f3, f4, f5), policy);
        }

        public CachePolicyConfig<T> field(
                Field<T, ?> f1, Field<T, ?> f2, Field<T, ?> f3, Field<T, ?> f4, Field<T, ?> f5,
                Field<T, ?> f6,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            return field(Set.of(f1, f2, f3, f4, f5, f6), policy);
        }

        public CachePolicyConfig<T> field(
                Field<T, ?> f1, Field<T, ?> f2, Field<T, ?> f3, Field<T, ?> f4, Field<T, ?> f5,
                Field<T, ?> f6, Field<T, ?> f7,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            return field(Set.of(f1, f2, f3, f4, f5, f6, f7), policy);
        }

        public CachePolicyConfig<T> field(
                Field<T, ?> f1, Field<T, ?> f2, Field<T, ?> f3, Field<T, ?> f4, Field<T, ?> f5,
                Field<T, ?> f6, Field<T, ?> f7, Field<T, ?> f8,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            return field(Set.of(f1, f2, f3, f4, f5, f6, f7, f8), policy);
        }

        public CachePolicyConfig<T> field(
                Field<T, ?> f1, Field<T, ?> f2, Field<T, ?> f3, Field<T, ?> f4, Field<T, ?> f5,
                Field<T, ?> f6, Field<T, ?> f7, Field<T, ?> f8, Field<T, ?> f9,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            return field(Set.of(f1, f2, f3, f4, f5, f6, f7, f8, f9), policy);
        }

        public CachePolicyConfig<T> field(
                Field<T, ?> f1, Field<T, ?> f2, Field<T, ?> f3, Field<T, ?> f4, Field<T, ?> f5,
                Field<T, ?> f6, Field<T, ?> f7, Field<T, ?> f8, Field<T, ?> f9, Field<T, ?> f10,
                Function<CachePolicy.CustomCachePolicy<T>, CachePolicy.CustomCachePolicy<T>> policy
        ) {
            return field(Set.of(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10), policy);
        }

        public void orElse(Function<CachePolicy.StaticCachePolicy, CachePolicy.StaticCachePolicy> policy) {
            build(policy.apply(CachePolicy.ALL()));
        }

        private void build(@Nullable CachePolicy.StaticCachePolicy orElse) {
            Map<Field<T, ?>, CachePolicy<?>> builtMap = new HashMap<>();

            for (var field : table.getAllFields()) {
                List<CachePolicy<?>> policy = mapStore.get(field);

                if (policy != null && !policy.isEmpty()) {
                    builtMap.put(field, policy.getLast());
                    continue;
                }

                if (orElse != null) {
                    builtMap.put(field, orElse);
                    continue;
                }

                Class<?> raw = field.getTypeToken().getRawType();
                boolean isBlob = raw == byte[].class
                        || raw == Byte[].class
                        || java.nio.ByteBuffer.class.isAssignableFrom(raw)
                        || field instanceof BinaryField<?, ?>;

                builtMap.put(field,
                        new CachePolicy.CustomCachePolicy<>(Set.of(field))
                                .withCachePolicy(isBlob
                                        ? CachePolicy.Policy.NONE
                                        : CachePolicy.Policy.EAGER
                                )
                );
            }

            Set<CachePolicy.PolicyMap<T>> mappedValues = new HashSet<>();
            for (var entry : builtMap.entrySet()) {
                mappedValues.add(
                        new CachePolicy.PolicyMap<>(entry.getKey())
                                .duplicate(entry.getValue())
                );
            }
            consumer.accept(CachePolicy.of(mappedValues));
        }
    }

    public final CachePolicyConfig<E> modifyCachePolicy() {
        return new CachePolicyConfig<>(table, p -> this.cachePolicy = p);
    }

    public final TableConfig<E> cachePolicy(CachePolicy.StaticCachePolicy policy) {
        this.cachePolicy = policy;
        return this;
    }

    public final TableConfig<E> cachePolicy(CachePolicy.CustomCachePolicy<E> policy) {
        this.cachePolicy = policy;
        return this;
    }

    @SafeVarargs
    public final TableConfig<E> autoCacheOnGet(Field<E, ?> field, Field<E, ?>... fields) {
        if (autoCacheOnGet == null) {
            autoCacheOnGet = new HashSet<>();
        }
        autoCacheOnGet.clear();
        autoCacheOnGet.add(field);
        autoCacheOnGet.addAll(List.of(fields));
        return this;
    }

    public final TableConfig<E> dontAutoCacheOnGet() {
        autoCacheOnGet = new HashSet<>();
        return this;
    }

    public final TableConfig<E> setTableName(String name) {
        this.name = name;
        return this;
    }
}
