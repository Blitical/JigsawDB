# JigsawDB v1.0.0-beta.10

Minor update to add better logger support and fix tests.

## Changelog

- Fixed tests not properly dropping entries when supposed to
- Use SLF4j as our default logger implementation
    - You can still override it via `JigsawDBConfig`