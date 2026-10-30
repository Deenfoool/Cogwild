# Cogwild: Mechanical Fauna

Рабочий прототип для **Minecraft 1.20.1 / Forge 47.4.28 / Java 17**, версия **0.3.0**.

## Что уже работает

- Мех-муравей: приручение и управление на основе лошади, посадка и высадка, ход вперёд и назад, повороты, заряжаемый прыжок, анимации и отдельные опоры шести лап под рельеф.
- Чемодан-планер: автоматическое раскрытие при опасном падении, складывание после приземления, рецепт, окраска и износ.
- Lathe Soft Aged и Crusher Refined Console Aged: устанавливаемые станки два на один блок, состаренные текстуры, анимации, коллизия, поворот при размещении и предметы в творческой вкладке. Редстоун переключает визуальное питание.

Переработка ресурсов, машинные инвентари, приём выброшенных предметов, FE-энергия и трубы пока не реализованы. Анимации обработки сохранены в ресурсах.

## Установка

Скачать JAR из [Releases](https://github.com/Deenfoool/Cogwild/releases), положить в `mods` профиля Forge 1.20.1. На сервере и клиентах должна быть одна версия. Дополнительная библиотека анимаций не нужна.

Текущий прототип сохраняет Mod ID `suitcaseglider` и имя архива `salum-mech-ant` для совместимости с уже созданными мирами. При обновлении заменять старый JAR. Целевой namespace проекта `cogwild` требует отдельной миграции сохранений и ещё не включён.

Игровые ID: `suitcaseglider:mech_ant`, `suitcaseglider:glider`, `suitcaseglider:lathe`, `suitcaseglider:crusher`.

## Сборка

Java 17. Из корня репозитория:

```sh
./gradlew build
```

Windows: `gradlew.bat build`. Результат: `build/libs/salum-mech-ant-0.3.0.jar`.

`./gradlew test` запускает 21 модульный тест. Отдельный серверный прогон прошёл 80 проверок станков: установка во всех направлениях, обе клетки, питание и корректное выпадение одного предмета. Отчёты — в [docs/verification](docs/verification).

## Исходники моделей и документация

- [Токарный станок](art_source/blockbench/blocks/Lathe-Soft-Aged.bbmodel)
- [Дробилка](art_source/blockbench/blocks/Crusher-Refined-Console-Aged.bbmodel)
- [Мех-муравей](art_source/blockbench/entities/Mech-Ant-Three-Section-Solid.bbmodel)
- [Управление и подробности прототипа](docs/prototype-0.3.0.md)

Рабочие модели `.bbmodel` хранятся в `art_source` и не попадают в игровой JAR.

Режимы `-PmechDev`, `-PmechQa`, `-PmechMotionQa`, `-PmechTerrainQa` и `-PmachineSmoke` предназначены только для отдельных тестовых профилей; они создают тестовые площадки или управляют игроком. Обычная установка JAR их не запускает. Для ручной проверки можно использовать обычный `./gradlew runClient`.

## Повторный экспорт моделей

Экспортёры используют Python 3 и стандартную библиотеку:

```sh
python tools/export_machines.py
python tools/export_mech_ant.py
./gradlew build
```

Они берут `.bbmodel` из `art_source/blockbench` и обновляют готовые геометрию, анимации и текстуры в `src/main/resources/assets/suitcaseglider`.
