# JigsawDB v1.0.0-beta.9
> [!WARNING]
> **JigsawDB is currently in its public beta release; there may be issues**  
> If you encounter any issues, please report them in our Discord [here](https://discord.gg/nKAZa796ua)

{SUMMARY}

## Changelog

- Renamed release bot from "JigsawDB Bot" to "JigsawDB"
- Added support for `MariaDB` and `PostgreSQL`
    - We did this by reworking the entire hierarchy of databases
    - <sup>**POSTGRESQL ONLY**</sup> `PostgreSQL` does not support `NUL` characters. These will be stripped and a warning will be thrown.
    - We have added a `ParseType.BINARY` to combat this; this parses any object into a binary object.
        - This can be used for any database type.
- Fixed bug where if no error is thrown if superclass `Table<K, V>` is not properly extended
