# Именование

## Общие правила

- Mod ID: `cogwild`
- registry ID и файлы: `snake_case`
- Java-классы: `PascalCase`
- Java-поля и методы: `camelCase`
- только латинские символы в технических именах

## Пример блока

```text
Название: Механический обрабатывающий станок
ID: machining_station
Полный ID: cogwild:machining_station
Java: MachiningStationBlock
Block Entity: MachiningStationBlockEntity
Geo: machining_station.geo.json
Animation: machining_station.animation.json
Texture: machining_station.png
Translation: block.cogwild.machining_station
```

## Префиксы переводов

```text
item.cogwild.steel_ingot
block.cogwild.machine_casing
entity.cogwild.mech_ant
fluid_type.cogwild.diesel
itemGroup.cogwild
```

## Запрещённые варианты

```text
Machining_Station
machining station
mechanical-station
МеханическийСтанок
cogwild:machining_station.json
```

Двоеточие используется только в игровом идентификаторе, но не в имени файла.
