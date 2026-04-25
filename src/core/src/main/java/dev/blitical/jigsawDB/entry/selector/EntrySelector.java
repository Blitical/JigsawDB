package dev.blitical.jigsawDB.entry.selector;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.cache.CacheHandler;
import dev.blitical.jigsawDB.cache.CachePolicy;
import dev.blitical.jigsawDB.entry.Entry;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.selector.condition.Condition;
import dev.blitical.jigsawDB.entry.selector.condition.Condition.NodeType;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager.LogicalCondition;
import dev.blitical.jigsawDB.entry.selector.util.OrderType;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.value.ExecutableFuture;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EntrySelector<T extends Table<T, P>, P> {

    final ConnectedDatabase.Exposed database;
    final Table<T, ?> table;
    List<SortBy> sort = new ArrayList<>();
    Function<List<Entry<T, P>>, List<Entry<T, P>>> sortFunction = v -> v;
    Set<Field<T, ?>> cacheFields = new HashSet<>();
    Integer limit = null;

    Condition<T> root = null;
    LogicalCondition<T> currentAnd = null;

    @CheckReturnValue
    EntrySelector(ConnectedDatabase.Exposed database, Table<T, ?> table) {
        this.database = database;
        this.table = table;
    }

    @CheckReturnValue
    EntrySelector(WithWhere<T, P> where) {
        this.database = where.selector.database;
        this.table = where.selector.table;
        this.sort = where.selector.sort;
        this.sortFunction = where.selector.sortFunction;
        this.cacheFields = where.selector.cacheFields;
        this.limit = where.selector.limit;
        this.root = where.selector.root;
    }

    public record SortBy(
            Field<?, ?> field,
            OrderType type
    ) {
    }

    @CheckReturnValue
    public EntrySelector<T, P> sort(
            Field<T, ?> field,
            OrderType type
    ) {
        sort.add(new SortBy(field, type));
        return this;
    }

    @CheckReturnValue
    public EntrySelector<T, P> sort(Function<List<Entry<T, P>>, List<Entry<T, P>>> sortFunction) {
        this.sortFunction = sortFunction;
        return this;
    }

    @SafeVarargs
    @CheckReturnValue
    public final EntrySelector<T, P> cacheFields(Field<T, ?> field, Field<T, ?>... fields) {
        this.cacheFields = Arrays.stream(fields).collect(Collectors.toSet());
        this.cacheFields.add(field);
        return this;
    }

    @CheckReturnValue
    public EntrySelector<T, P> limit(@Range(from = 1, to = Integer.MAX_VALUE) int limit) {
        this.limit = limit;
        return this;
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public <F extends Field<T, ?>> ExecutableFuture<@NotNull List<@NotNull Entry<T, P>>> fetch() {
        return new ExecutableFuture<>(database, () -> {
            Set<Field<T, ?>> fields = new HashSet<>();
            fields.add(table.getPrimaryColumn());
            fields.addAll(Arrays.stream(CacheHandler.getFieldsByPolicy(database, table, CachePolicy.Policy.EAGER)).collect(Collectors.toSet()));
            fields.addAll(cacheFields);
            try {
                List<Entry<T, P>> entries = database.driver().getSpecified(
                        database,
                        (Table<T, P>) table,
                        root,
                        sort,
                        limit,
                        fields
                );
                sortFunction.apply(entries);
                return entries;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    void addCondition(NodeType type, Condition<T> newCondition) {
        if (root == null) {
            root = newCondition;
            return;
        }

        switch (type) {
            case AND, COMPARISON -> {
                if (currentAnd != null) {
                    currentAnd.add(newCondition);
                    return;
                }

                LogicalCondition<T> and = new LogicalCondition<>(NodeType.AND);
                and.add(root);
                and.add(newCondition);

                root = and;
                currentAnd = and;
            }

            case OR -> {
                LogicalCondition<T> or = new LogicalCondition<>(NodeType.OR);
                or.add(root);
                or.add(newCondition);

                root = or;
                currentAnd = null;
            }

            case NOT -> {
                LogicalCondition<T> not = new LogicalCondition<>(NodeType.NOT);
                not.add(newCondition);

                addCondition(NodeType.AND, not);
            }
        }
    }

    /**
     * This is a return method used by {@link WithWhere} to basically <br>
     * suppress {@code Result of 'EntrySelector#...' is ignored } errors
     */
    void nothing() {
    }
}
