# Cogwild 0.6.0 machine integration

Eight approved source models are registered as placeable blocks and inventory items. Added rolling_mill, quenching_bath, air_separator, air_compressor, industrial_press, diesel_generator. Existing lathe and crusher IDs remain stable.

Validation: 31 JUnit tests, 522 dedicated-server machine checks (all models and four orientations), 26 cable-network server checks, and an isolated Forge client scene rendering all eight world models and all eight items. Final client load has no missing-texture warnings. Vanilla water sprites render inside the bath's animated fluid volume. The generator rig retains its 20-unit / 1.25-block height and four output anchors.

Placement reserves the complete width and vertical extent. Removal from a secondary cell clears the structure and drops exactly one item; creative removal drops none. Powered visual clips are selected per model. Generated meshes include triangulated cap polygons and emissive texture flags.

Commands: `gradlew test build --offline`, `gradlew runServer -PmachineSmoke --offline`, `gradlew runServer -PcableSmoke --offline`, `gradlew test build runClient -PmachineQa --offline` (Java 17).

Scope: model integration, placement/collision, item rendering, crafting, drops, power animation and cable anchor mapping. Processing inventories/recipes, actual diesel generation and per-outlet voltage simulation are not implemented. Existing fuel-generator energy behavior is preserved.
