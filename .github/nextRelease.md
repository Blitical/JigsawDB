# JigsawDB v1.0.0-beta.11

This version places a primary focus on bucketing and batching.  
Such optimisations must be implemented for the longevity and scalability of any codebase.

## Changelog

- Added Buckets and Batching
    - You can now batch requests through `Entry#batch()`
        - This allows for batch setting and getting of values efficiently.
        - This only works for **one entry inside a table**.
    - You can now also create a new Bucket through `ConnectedDatabase#createBucket()`
        - This allows for executing many requests across different tables and entries
        - **All requests must be in the same database for this to work.**
        - You can add `JigsawDBAction` objects to a bucket
            - These can be received through `Entry#bucketGetter()#<YOUR-DESIRED-METHOD>`
    - Lastly, you can now directly create entires with pre-defined values using `InitialValueExecutor`
        - The executor is specified here: ```ConnectedDatabase#createEntry(CoolTable.class, "Blitical", iv -> ...)```
        - And you can call it by `iv -> iv.set(CoolTableFields.rank, Rank.ADMIN).build()`
- Miscellaneous:
    - Added a new `DriverType` for driver types
- Contributor Notes:
    - Improved readability of the `Entry` class
    - Added a new `JigsawDBAction` class for actions returned by drivers
    - Made `ExecutableFuture` suppliers accept with exceptions (no need to manually surround with try-catch anymore)
