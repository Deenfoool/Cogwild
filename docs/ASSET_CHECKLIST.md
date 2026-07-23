# Чек-лист игровых ресурсов

Используйте этот список перед добавлением нового объекта в код.

## Общие правила

- технический ID записан в `snake_case`;
- имя совпадает во всех Java- и JSON-файлах;
- файлы не содержат пробелов, русских букв и двоеточий;
- исходники `.bbmodel` лежат в `art_source/`, а не в `src/main/resources`;
- экспортированные игровые ресурсы лежат в `assets/cogwild`.

## Обычный предмет

- `textures/item/<id>.png`;
- `models/item/<id>.json`;
- перевод `item.cogwild.<id>` в `ru_ru.json` и `en_us.json`;
- регистрация в `ModItems`;
- рецепт или другой способ получения;
- запись в `docs/REGISTRY_IDS.md`.

## Обычный блок

- `textures/block/<id>.png`;
- `models/block/<id>.json`;
- `blockstates/<id>.json`;
- `models/item/<id>.json`;
- перевод `block.cogwild.<id>`;
- регистрация блока и `BlockItem`;
- loot table;
- рецепт или другой способ получения;
- проверка подходящего инструмента.

## GeckoLib-механизм

- `geo/block/<id>.geo.json`;
- `animations/block/<id>.animation.json`;
- `textures/block/<id>.png`;
- подвижные части разделены на отдельные bones;
- присутствуют состояния `idle` и `working`, если нужна анимация;
- модель, рендерер и Block Entity используют одинаковый ID;
- модель проверена при разных направлениях размещения.

## GeckoLib-сущность

- `geo/entity/<id>.geo.json`;
- `animations/entity/<id>.animation.json`;
- `textures/entity/<id>.png`;
- pivot-точки конечностей проверены;
- имя корневой bone стабильно;
- анимации не сдвигают сущность относительно hitbox;
- исходник Blockbench сохранён в `art_source/blockbench/entities/`.

## Перед Pull Request

- проект собирается;
- JSON-файлы валидны;
- отсутствуют пропавшие текстуры;
- добавлены оба перевода;
- новый ID внесён в документацию;
- в PR приложены скриншоты модели или объекта в игре;
- временные файлы и резервные копии не попали в коммит.
