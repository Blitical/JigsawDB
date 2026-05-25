package dev.blitical.jigsawDB.entry.selector;

import dev.blitical.jigsawDB.entry.fields.GenericField;
import dev.blitical.jigsawDB.entry.selector.condition.Condition.NodeType;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager.ComparisonCondition;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager.CustomCondition;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager.InCondition;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager.LikeCondition;
import dev.blitical.jigsawDB.entry.selector.util.ComparisonType;
import dev.blitical.jigsawDB.table.Table;
import org.jetbrains.annotations.CheckReturnValue;

import java.util.Set;
import java.util.function.Function;

public final class Comparison<T extends Table<T, P>, P, V> {

    private final WhereSelector<T, P> selector;
    private final GenericField<T, V> field;
    final NodeType type;

    Comparison(WhereSelector<T, P> selector, GenericField<T, V> field) {
        this.selector = selector;
        this.field = field;
        this.type = NodeType.COMPARISON;
    }

    Comparison(WhereSelector<T, P> selector, GenericField<T, V> field, NodeType type) {
        this.selector = selector;
        this.field = field;
        this.type = type;
    }

    @CheckReturnValue
    public WhereSelector<T, P> eq(V value) {
        selector.addCondition(
                type,
                new ComparisonCondition<>(field, ComparisonType.EQUALS, value)
        );
        return selector;
    }

    @CheckReturnValue
    public WhereSelector<T, P> not_eq(V value) {
        selector.addCondition(
                type,
                new ComparisonCondition<>(field, ComparisonType.NOT_EQUALS, value)
        );
        return selector;
    }

    @CheckReturnValue
    public WhereSelector<T, P> like(String value) {
        selector.addCondition(
                type,
                new LikeCondition<>(field, value)
        );
        return selector;
    }

    @SafeVarargs
    @CheckReturnValue
    public final WhereSelector<T, P> in(V... values) {
        Set<V> immutable = Set.of(values);
        selector.addCondition(
                type,
                new InCondition<>(field, immutable)
        );
        return selector;
    }

    @CheckReturnValue
    public final WhereSelector<T, P> custom(Function<String, String> sqlFunction, Object... args) {
        selector.addCondition(
                type,
                new CustomCondition<>(field, sqlFunction, args)
        );
        return selector;
    }
}
