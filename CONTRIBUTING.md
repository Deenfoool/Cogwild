# Вклад в Cogwild

Спасибо за помощь в разработке **Cogwild: Mechanical Fauna**.

## Ветки

- `main` — стабильная история проекта;
- `develop` — текущая разработка;
- новые изменения желательно делать в отдельных ветках и отправлять через Pull Request в `develop`.

## Именование

- registry ID: `snake_case`;
- Java-классы: `PascalCase`;
- пакеты и файлы ресурсов: строчные латинские буквы;
- Mod ID: `cogwild`.

Пример:

```text
Java: MachiningStationBlock
ID: cogwild:machining_station
Model: machining_station.geo.json
Texture: machining_station.png
Translation: block.cogwild.machining_station
```

## Коммиты

Используйте короткие понятные сообщения на английском:

```text
Add steel ingot
Fix machine casing loot table
Add mech ant model
```

## Перед Pull Request

- проект собирается командой `./gradlew build`;
- добавлены русская и английская локализации;
- для блока есть blockstate, item model и loot table;
- для игрового контента указан технический ID;
- рабочие исходники `.bbmodel` лежат в `art_source/`;
- экспортированные игровые файлы лежат в `src/main/resources/`;
- папки `run/`, `build/`, логи и настройки IDE не добавлены в Git.

## Модели и текстуры

- блоки: `art_source/blockbench/blocks/`;
- существа: `art_source/blockbench/entities/`;
- предметы: `art_source/blockbench/items/`;
- игровые GeckoLib-модели: `assets/cogwild/geo/`;
- анимации: `assets/cogwild/animations/`;
- текстуры: `assets/cogwild/textures/`.

Не добавляйте чужие текстуры, модели, музыку или код без разрешения и указания лицензии.
