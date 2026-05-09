# JigsawDB v1.0.0-beta.12

This version focuses on more optimizations, specifically adding `ColumnTypes` for specifying your own column type for efficiency.

## Changelog

- Added an alias for `ConnectedDatabase#createTable(table, primary, initialValExec)`
  - This is `ConnectedDatabase#createTable(table, primary)` (`initialValExec` is null here)
- Added a new `ColumnConfig` option named `columnType(...)`
  - This allows you to specify a column type yourself, allowing for more optimized columns based on your use case
    - For example, `columnType(ColumnTypes.varchar(20))` will force the column to have `varChar(20)` (maximum 20 characters)
  - Thus, for `MariaDB`, `MySQL`, and `PostgreSQL`, we have optimized column types for your needs
