package dev.blitical.jigsawDB.entry.selector;

import dev.blitical.jigsawDB.entry.fields.GenericField;
import dev.blitical.jigsawDB.entry.fields.NumberField;
import dev.blitical.jigsawDB.entry.selector.condition.Condition;
import dev.blitical.jigsawDB.entry.selector.condition.Condition.NodeType;
import dev.blitical.jigsawDB.table.Table;
import org.jetbrains.annotations.CheckReturnValue;

import java.util.function.Function;

public class WhereSelector<T extends Table<T, P>, P> {

    final EntrySelector<T, P> selector;

    WhereSelector(EntrySelector<T, P> selector) {
        this.selector = selector;
    }

    public EntrySelector<T, P> endWhere() {
        return selector;
    }

    public static class SelectedWhere {
        protected SelectedWhere() {
        }
    }

    public SelectedWhere select() {
        return new SelectedWhere();
    }

    void addCondition(NodeType type, Condition<T> condition) {
        selector.addCondition(type, condition);
    }

    public <E> Comparison<T, P, E> and(GenericField<T, E> field) {
        return new Comparison<>(this, field, NodeType.AND);
    }

    public <E> Comparison<T, P, E> or(GenericField<T, E> field) {
        return new Comparison<>(this, field, NodeType.OR);
    }

    public <E> Comparison<T, P, E> not(GenericField<T, E> field) {
        return new Comparison<>(this, field, NodeType.NOT);
    }

    public <E> NumberComparison<T, P, E> and(NumberField<T, E> field) {
        return new NumberComparison<>(this, field, NodeType.AND);
    }

    public <E> NumberComparison<T, P, E> or(NumberField<T, E> field) {
        return new NumberComparison<>(this, field, NodeType.OR);
    }

    public <E> NumberComparison<T, P, E> not(NumberField<T, E> field) {
        return new NumberComparison<>(this, field, NodeType.NOT);
    }

    public static class LogicalWhere<T extends Table<T, P>, P> {

        private final WhereSelector<T, P> where;

        protected LogicalWhere(WhereSelector<T, P> where) {
            this.where = where;
        }

        @CheckReturnValue
        public <E> Comparison<T, P, E> where(GenericField<T, E> field) {
            return new Comparison<>(where, field);
        }

        @CheckReturnValue
        public <E> NumberComparison<T, P, E> where(NumberField<T, E> field) {
            return new NumberComparison<>(where, field);
        }
    }

    public WhereSelector<T, P> and(Function<LogicalWhere<T, P>, SelectedWhere> builder) {
        return group(NodeType.AND, builder);
    }

    public WhereSelector<T, P> or(Function<LogicalWhere<T, P>, SelectedWhere> builder) {
        return group(NodeType.OR, builder);
    }

    public WhereSelector<T, P> not(Function<LogicalWhere<T, P>, SelectedWhere> builder) {
        return group(NodeType.NOT, builder);
    }

    private WhereSelector<T, P> group(NodeType type, Function<LogicalWhere<T, P>, SelectedWhere> builder) {
        EntrySelector<T, P> nestedSelector = new EntrySelector<>(selector.database, selector.table);
        WhereSelector<T, P> nestedWhere = new WhereSelector<>(nestedSelector);
        LogicalWhere<T, P> nested = new LogicalWhere<>(nestedWhere);

        builder.apply(nested);
        Condition<T> nestedRoot = nestedSelector.root;

        if (nestedRoot == null)
            return this;

        selector.addCondition(type, nestedRoot);
        return this;
    }
}
