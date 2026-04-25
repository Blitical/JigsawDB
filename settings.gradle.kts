rootProject.name = "JigsawDB"
include("core", "processor", "test")

project(":core").projectDir = file("src/core")
project(":processor").projectDir = file("src/processor")
project(":test").projectDir = file("src/test")
