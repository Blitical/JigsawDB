# JigsawDB v1.0.0-beta.18

Bug fixes

## Changelog

- **IMPORTANT:** Unshaded respective database handles from the project
  - You must now manually shade in the database service you will be using and hook onto it with our driver
- Cached table config, reflected column fields, and primary-column lookup in `Table`.
  - It now also caches lookup by column name instead of field
- Cleaned CachedMap into named nested cache maps and made the all-entry tracker concurrent.
- Fixed cache policy fallback/null handling and duplicate() copying.
- Fixed custom selector sorting being ignored.
- Encoded condition values before binding SQL args, normalized ORDER BY identifiers, and avoided unnecessary rebuilds when columns are merely missing.
- Improved PostgreSQL schema introspection for primary keys/type comparisons.
- Cleaned SQLite table metadata quoting and small decompiler-style leftovers.
- Fixed duplicate default/initial insert values so explicit initial values override defaults.
