package dev.blitical.jigsawDB.entry.selector.condition;

import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.fields.GenericField;
import dev.blitical.jigsawDB.entry.fields.NumberField;
import dev.blitical.jigsawDB.entry.selector.util.ComparisonType;
import dev.blitical.jigsawDB.entry.selector.util.NumberComparisonType;
import dev.blitical.jigsawDB.table.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConditionManager {

    public static class ComparisonCondition<T extends Table<T, ?>, E> extends Condition<T> {

        public final GenericField<T, E> field;
        public final ComparisonType type;
        public final E value;

        public ComparisonCondition(GenericField<T, E> field, ComparisonType type, E value) {
            super();
            this.field = field;
            this.type = type;
            this.value = value;
        }
    }

    public static class NumberComparisonCondition<T extends Table<T, ?>, E> extends Condition<T> {

        public final NumberField<T, E> field;
        public final NumberComparisonType type;
        public final E value;

        public NumberComparisonCondition(NumberField<T, E> field, NumberComparisonType type, E value) {
            super();
            this.field = field;
            this.type = type;
            this.value = value;
        }
    }

    public static class BetweenCondition<T extends Table<T, ?>, E> extends Condition<T> {

        public final NumberField<T, E> field;
        public final E min;
        public final E max;

        public BetweenCondition(NumberField<T, E> field, E min, E max) {
            super();
            this.field = field;
            this.min = min;
            this.max = max;
        }
    }

    public static class LikeCondition<T extends Table<T, ?>, E> extends Condition<T> {

        public final GenericField<T, E> field;
        public final String match;

        public LikeCondition(GenericField<T, E> field, String match) {
            super();
            this.field = field;
            this.match = match;
        }
    }

    public static class InCondition<T extends Table<T, ?>, E> extends Condition<T> {

        public final Field<T, E> field;
        public final Set<E> values;

        // Don't directly expose Field in one constructor;
        // it  will result in inconsistencies with PrimaryField
        public InCondition(GenericField<T, E> field, Set<E> values) {
            super();
            this.field = field;
            this.values = values;
        }

        public InCondition(NumberField<T, E> field, Set<E> values) {
            super();
            this.field = field;
            this.values = values;
        }
    }

    public static class LogicalCondition<T extends Table<T, ?>> extends Condition<T> {

        public final List<Condition<T>> children = new ArrayList<>();

        public LogicalCondition(NodeType type) {
            super(type);
            if (type == NodeType.COMPARISON)
                throw new IllegalArgumentException();
        }

        public void add(Condition<T> child) {
            children.add(child);
        }

        public List<Condition<T>> children() {
            return children;
        }
    }
}
