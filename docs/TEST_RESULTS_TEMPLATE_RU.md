# SAOIM — протокол проверки этапа 11

Дата:

Ветка и commit SHA:

Windows / сборка:

JDK / Maven:

Видеокарта и драйвер:

## Автоматические проверки

| Проверка | Результат | Примечание |
|---|---|---|
| `mvn clean verify` | PASS / FAIL | |
| Unit tests | PASS / FAIL | |
| JaCoCo report создан | PASS / FAIL | |
| GitHub Actions Windows | PASS / FAIL | |
| GitHub Actions Ubuntu | PASS / FAIL | |
| MySQL integration profile | PASS / FAIL / N/A | |

## Ручные проверки

| Сценарий | Результат | Примечание |
|---|---|---|
| Один монитор, DPI 100% | PASS / FAIL | |
| Второй монитор, DPI 125% | PASS / FAIL | |
| Смешанный DPI 100% + 150% | PASS / FAIL | |
| Жест на активном рабочем столе | PASS / FAIL | |
| Жест при активном стороннем окне игнорируется | PASS / FAIL | |
| Жест из профиля возвращает меню | PASS / FAIL | |
| Esc: диалог → уровень → меню → скрытие | PASS / FAIL | |
| ПКМ скрывает SAOIM без меню Windows | PASS / FAIL | |
| 50 быстрых повторных переходов | PASS / FAIL | |
| Глубокое дерево | PASS / FAIL | |
| 1,000 ярлыков | PASS / FAIL | |
| Удаление большого поддерева + Ctrl+Z | PASS / FAIL | |
| Ctrl+Shift+Z | PASS / FAIL | |
| Недоступные EXE/LNK/файл/папка | PASS / FAIL | |
| URL и Microsoft Store | PASS / FAIL | |
| Потеря MySQL во время работы | PASS / FAIL | |
| Восстановление после возврата MySQL | PASS / FAIL | |

## Производительность

Время открытия профиля:

Время перехода между категориями:

Максимальная загрузка CPU/GPU:

Наблюдаемые рывки или зависания:

## Обнаруженные дефекты

1.

## Итог

READY / NOT READY
