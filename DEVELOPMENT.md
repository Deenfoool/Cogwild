# Cogwild development status

## Fixed technical stack

- Minecraft: **1.21.1**
- Loader: **Minecraft Forge 52.1.0**
- Java: **21**
- Mod ID: `cogwild`
- Base package: `ru.deenfoool.cogwild`
- Animation library: **GeckoLib**
- Current version: `0.1.0-dev`

## Implemented in the bootstrap branch

- ForgeGradle project configuration;
- Forge and GeckoLib dependencies;
- Cogwild mod entry point;
- deferred item and block registries;
- first item: `cogwild:steel_ingot`;
- first block: `cogwild:machine_casing`;
- Russian and English translations;
- blockstate, block model and item models;
- machine casing recipe and loot table.

The temporary models reuse vanilla Minecraft textures. Original Cogwild textures will replace them later.

## Required before the first verified build

The repository still needs the official Gradle wrapper files from the Forge 1.21.1 MDK:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Binary wrapper files cannot be created reliably through the current GitHub text-file integration. Download the official Forge **1.21.1-52.1.0 MDK**, then copy the wrapper files into the repository.

After that, run:

```bash
./gradlew build
```

For an IDE run configuration:

```bash
./gradlew genIntellijRuns
```

or

```bash
./gradlew genEclipseRuns
```

## Definition of bootstrap complete

- [x] technical stack fixed;
- [x] build scripts added;
- [x] mod entry point added;
- [x] first item registered;
- [x] first block registered;
- [x] resources and translations added;
- [ ] official Gradle wrapper added;
- [ ] local build succeeds;
- [ ] Minecraft client starts with Cogwild loaded;
- [ ] CI build enabled.
