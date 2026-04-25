package dev.blitical.jigsawDB.entry.selector;

import dev.blitical.jigsawDB.ConnectedDatabase;
import dev.blitical.jigsawDB.entry.Entry;
import dev.blitical.jigsawDB.entry.Field;
import dev.blitical.jigsawDB.entry.fields.GenericField;
import dev.blitical.jigsawDB.entry.fields.NumberField;
import dev.blitical.jigsawDB.entry.selector.util.OrderType;
import dev.blitical.jigsawDB.table.Table;
import dev.blitical.jigsawDB.value.ExecutableFuture;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class WithWhere<T extends Table<T, P>, P> {

    final EntrySelector<T, P> selector;

    @CheckReturnValue
    @ApiStatus.Internal
    public WithWhere(ConnectedDatabase.Exposed database, Table<T, P> table) {
        this.selector = new EntrySelector<>(database, table);
    }

    @CheckReturnValue
    public <E> Comparison<T, P, E> where(GenericField<T, E> field) {
        return new Comparison<>(
                new WhereSelector<>(new EntrySelector<>(this)),
                field
        );
    }

    @CheckReturnValue
    public <E> NumberComparison<T, P, E> where(NumberField<T, E> field) {
        return new NumberComparison<>(
                new WhereSelector<>(new EntrySelector<>(this)),
                field
        );
    }

    @CheckReturnValue
    public final WithWhere<T, P> where(Consumer<WhereSelector.LogicalWhere<T, P>> builder) {
        WhereSelector<T, P> whereSelector = new WhereSelector<>(this.selector);
        WhereSelector.LogicalWhere<T, P> logicalWhere = new WhereSelector.LogicalWhere<>(whereSelector);
        builder.accept(logicalWhere);
        return this;
    }

    @CheckReturnValue
    public WithWhere<T, P> sort(
            Field<T, ?> field,
            OrderType type
    ) {
        this.selector.sort(field, type).nothing();
        return this;
    }

    @CheckReturnValue
    public WithWhere<T, P> sort(Function<List<Entry<T, P>>, List<Entry<T, P>>> sortFunction) {
        this.selector.sort(sortFunction).nothing();
        return this;
    }

    @SafeVarargs
    @CheckReturnValue
    public final WithWhere<T, P> cacheFields(Field<T, ?> field, Field<T, ?>... fields) {
        this.selector.cacheFields(field, fields).nothing();
        return this;
    }

    @CheckReturnValue
    public WithWhere<T, P> limit(@Range(from = 1, to = Integer.MAX_VALUE) int limit) {
        this.selector.limit(limit).nothing();
        return this;
    }

    @CheckReturnValue
    public ExecutableFuture<@NotNull List<@NotNull Entry<T, P>>> fetch() {
        return this.selector.fetch();
    }
}
