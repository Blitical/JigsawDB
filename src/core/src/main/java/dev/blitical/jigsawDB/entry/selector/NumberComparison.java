package dev.blitical.jigsawDB.entry.selector;

import dev.blitical.jigsawDB.entry.fields.NumberField;
import dev.blitical.jigsawDB.entry.selector.condition.Condition.NodeType;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager.BetweenCondition;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager.CustomCondition;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager.InCondition;
import dev.blitical.jigsawDB.entry.selector.condition.ConditionManager.NumberComparisonCondition;
import dev.blitical.jigsawDB.entry.selector.util.NumberComparisonType;
import dev.blitical.jigsawDB.table.Table;
import org.jetbrains.annotations.CheckReturnValue;

import java.util.Set;
import java.util.function.Function;

public class NumberComparison<T extends Table<T, P>, P, V> {

    private final WhereSelector<T, P> selector;
    private final NumberField<T, V> field;
    final NodeType type;

    NumberComparison(WhereSelector<T, P> selector, NumberField<T, V> field) {
        this.selector = selector;
        this.field = field;
        this.type = NodeType.COMPARISON;
    }

    NumberComparison(WhereSelector<T, P> selector, NumberField<T, V> field, NodeType type) {
        this.selector = selector;
        this.field = field;
        this.type = type;
    }

    @CheckReturnValue
    public WhereSelector<T, P> eq(V value) {
        selector.addCondition(
                type,
                new NumberComparisonCondition<>(field, NumberComparisonType.EQUALS, value)
        );
        return selector;
    }

    @CheckReturnValue
    public WhereSelector<T, P> notEq(V value) {
        selector.addCondition(
                type,
                new NumberComparisonCondition<>(field, NumberComparisonType.NOT_EQUAL, value)
        );
        return selector;
    }

    @CheckReturnValue
    public WhereSelector<T, P> greaterThan(V value) {
        selector.addCondition(
                type,
                new NumberComparisonCondition<>(field, NumberComparisonType.GREATER, value)
        );
        return selector;
    }

    @CheckReturnValue
    public WhereSelector<T, P> greaterThanOrEq(V value) {
        selector.addCondition(
                type,
                new NumberComparisonCondition<>(field, NumberComparisonType.GREATER_OR_EQUAL, value)
        );
        return selector;
    }

    @CheckReturnValue
    public WhereSelector<T, P> lessThan(V value) {
        selector.addCondition(
                type,
                new NumberComparisonCondition<>(field, NumberComparisonType.LESS, value)
        );
        return selector;
    }

    @CheckReturnValue
    public WhereSelector<T, P> lessThanOrEq(V value) {
        selector.addCondition(
                type,
                new NumberComparisonCondition<>(field, NumberComparisonType.LESS_OR_EQUAL, value)
        );
        return selector;
    }

    @CheckReturnValue
    public WhereSelector<T, P> between(V min, V max) {
        selector.addCondition(
                type,
                new BetweenCondition<>(field, min, max)
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
    public final WhereSelector<T, P> custom(Function<String, String> sqlFunction) {
        selector.addCondition(
                type,
                new CustomCondition<>(field, sqlFunction)
        );
        return selector;
    }
}
