## Changelog:

- Attempt to publish ShadowJar again instead of just the regular jar
    - It seems like it was trying to resolve a `JigsawDB:processor:1.0.0-beta.2` dependency on MavenCentral when this
      does not exist.
    - This is instead shaded in the jar uploaded to MavenCentral.
