package dev.blitical.jigsawDB.table;

public class ColumnConfig<T> {
    protected T defaultValue = null;
    protected boolean nullable = true;
    protected boolean unique = false;
    protected boolean autoIncrement = false;

    public DefinedColumnConfig<T> asDefinedConfig() {
        return new DefinedColumnConfig<>(
                defaultValue,
                nullable,
                unique,
                autoIncrement
        );
    }
}
