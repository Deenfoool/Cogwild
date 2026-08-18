# Архитектура Cogwild

## Слои

1. **Registry** — регистрация блоков, предметов, сущностей, меню, жидкостей и звуков.
2. **Common logic** — игровая логика, работающая на сервере и клиенте.
3. **Block Entity** — инвентарь, энергия, жидкости, прогресс и сохранение состояния машин.
4. **Recipe** — типы рецептов и сериализация данных.
5. **Network** — синхронизация интерфейсов и действий игрока.
6. **Client** — экраны, рендеры, GeckoLib-модели и визуальные эффекты.
7. **Data** — рецепты, теги, loot tables, достижения и генерация мира.

## Правило зависимостей

Клиентский код может зависеть от общей логики. Общая логика не должна импортировать клиентские классы.

```text
registry → block/item/entity
block → blockentity
blockentity → recipe/config/util
client → model/renderer/screen
```

## Одноблочная машина

Минимальный набор:

```text
MachineBlock
MachineBlockEntity
MachineMenu
MachineScreen
MachineRecipe
MachineModel
MachineRenderer
```

Не каждая машина обязана сразу иметь все классы. Простая статическая машина может начинаться с `Block` и `BlockEntity`.

## Многоблочная постройка

- контроллер хранит основное состояние;
- отдельный валидатор проверяет форму;
- порты отвечают за предметы, жидкости и энергию;
- визуальная модель не должна быть источником логики структуры.

## Механическая фауна

Для каждого меха планируются:

```text
Entity
AI / goals
Menu or interaction handler
GeckoLib model
Renderer
Animation controller
Fuel and cargo capability
```
