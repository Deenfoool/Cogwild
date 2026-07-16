# ⚙️ Cogwild: Mechanical Fauna

**Cogwild** — индустриальный мод для Minecraft о проектировании, производстве и эксплуатации механических существ. Игрок развивает металлургию, нефтепереработку и энергетику, строит многоблочные комплексы и собирает мехов из крупных компонентов по многоразовым чертежам.

> Репозиторий находится на ранней стадии подготовки. Версия Minecraft, загрузчик модов и окончательный набор зависимостей будут зафиксированы перед началом программного прототипирования.

## Основные направления

- механическая фауна: мехмуравей, робобуйвол и мехпчела;
- конечные подземные залежи нефти;
- дизель, бензин, керосин, биодизель и авиационное топливо;
- дробление руд, металлургия стали, латуни и вольфрама;
- одноблочные производственные механизмы;
- многоблочные буровые, печи, перерабатывающие комплексы и строительная верфь;
- заправка, ремонт и улучшение мехов;
- модели и анимации GeckoLib.

## Технические правила

- Mod ID: `cogwild`
- технические имена пишутся в `snake_case`;
- только строчные латинские буквы, цифры и `_`;
- полный игровой ID: `cogwild:machining_station`;
- имя файла: `machining_station.json`.

## Иерархия проекта

```text
Cogwild/
├─ build.gradle
├─ settings.gradle
├─ gradle.properties
├─ gradlew
├─ gradlew.bat
├─ gradle/
│  └─ wrapper/
│
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ ru/deenfoool/cogwild/
│  │  │     ├─ Cogwild.java
│  │  │     ├─ registry/
│  │  │     ├─ block/
│  │  │     │  ├─ machine/
│  │  │     │  ├─ pipe/
│  │  │     │  └─ multiblock/
│  │  │     ├─ blockentity/
│  │  │     │  ├─ machine/
│  │  │     │  └─ multiblock/
│  │  │     ├─ item/
│  │  │     │  ├─ material/
│  │  │     │  ├─ component/
│  │  │     │  ├─ blueprint/
│  │  │     │  └─ armor/
│  │  │     ├─ entity/
│  │  │     │  ├─ mech/
│  │  │     │  └─ ai/
│  │  │     ├─ fluid/
│  │  │     ├─ recipe/
│  │  │     ├─ multiblock/
│  │  │     ├─ menu/
│  │  │     ├─ network/
│  │  │     ├─ config/
│  │  │     ├─ util/
│  │  │     └─ client/
│  │  │        ├─ model/
│  │  │        ├─ renderer/
│  │  │        ├─ screen/
│  │  │        └─ animation/
│  │  │
│  │  └─ resources/
│  │     ├─ META-INF/
│  │     ├─ assets/cogwild/
│  │     │  ├─ lang/
│  │     │  ├─ blockstates/
│  │     │  ├─ models/block/
│  │     │  ├─ models/item/
│  │     │  ├─ geo/block/
│  │     │  ├─ geo/entity/
│  │     │  ├─ geo/item/
│  │     │  ├─ geo/armor/
│  │     │  ├─ animations/block/
│  │     │  ├─ animations/entity/
│  │     │  ├─ animations/item/
│  │     │  ├─ animations/armor/
│  │     │  ├─ textures/block/
│  │     │  ├─ textures/item/
│  │     │  ├─ textures/entity/
│  │     │  ├─ textures/armor/
│  │     │  ├─ textures/gui/
│  │     │  ├─ textures/fluid/
│  │     │  ├─ textures/particle/
│  │     │  ├─ sounds/
│  │     │  └─ particles/
│  │     │
│  │     └─ data/cogwild/
│  │        ├─ recipe/
│  │        ├─ loot_table/blocks/
│  │        ├─ loot_table/entities/
│  │        ├─ advancement/
│  │        ├─ tags/block/
│  │        ├─ tags/item/
│  │        ├─ tags/fluid/
│  │        ├─ worldgen/
│  │        └─ structures/
│  │
│  └─ generated/resources/
│
├─ art_source/
│  ├─ blockbench/blocks/
│  ├─ blockbench/entities/
│  ├─ blockbench/items/
│  ├─ blockbench/armor/
│  ├─ concepts/
│  ├─ textures_source/
│  └─ ui_source/
│
├─ docs/
└─ run/
```

## Где хранить экспорт GeckoLib

### Одноблочный механизм

```text
assets/cogwild/geo/block/machining_station.geo.json
assets/cogwild/animations/block/machining_station.animation.json
assets/cogwild/textures/block/machining_station.png
```

### Механическое существо

```text
assets/cogwild/geo/entity/mech_ant.geo.json
assets/cogwild/animations/entity/mech_ant.animation.json
assets/cogwild/textures/entity/mech_ant.png
```

### Анимированный предмет

```text
assets/cogwild/geo/item/diesel_engine.geo.json
assets/cogwild/animations/item/diesel_engine.animation.json
assets/cogwild/textures/item/diesel_engine.png
```

Для обычных слитков, пластин, пыли и шестерён GeckoLib не требуется:

```text
assets/cogwild/textures/item/steel_ingot.png
assets/cogwild/models/item/steel_ingot.json
```

## Многоблочные постройки

Многоблочная машина состоит из отдельных блоков, а контроллер проверяет конструкцию и управляет её работой.

```text
block/multiblock/
├─ BlastFurnaceControllerBlock.java
├─ BlastFurnaceCasingBlock.java
├─ BlastFurnaceInputHatchBlock.java
├─ BlastFurnaceOutputHatchBlock.java
└─ BlastFurnaceEnergyPortBlock.java

multiblock/
├─ MultiblockPattern.java
├─ MultiblockValidator.java
└─ BlastFurnaceStructure.java
```

## Рабочие исходники

Файлы `.bbmodel`, концепт-арты, PSD и черновые текстуры хранятся в `art_source/` и не должны попадать в готовый JAR.

```text
art_source/blockbench/blocks/machining_station.bbmodel
art_source/blockbench/entities/mech_ant.bbmodel
art_source/concepts/mech_bee_concept.png
```

## Статус

Проектирование систем, ресурсов, механизмов и мехов продолжается. Первым программным этапом станет регистрация базовых предметов и блоков, после чего будут добавлены машины, жидкости, многоблочные структуры и механическая фауна.
