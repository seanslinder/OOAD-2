# Лабораторная работа №2 (C++)

## Тема

Применение структурного паттерна **Декоратор** в веб-приложении с C++ логикой.

## Предметная область

Мини-приложение «Пиццерия», где пользователь выбирает базовую пиццу и добавки.

- Базовые пиццы: итальянская, болгарская.
- Добавки (декораторы): томаты, сыр, оливки, грибы.
- Результат: динамически формируются название итоговой пиццы и её стоимость.

## Технологии

- C++17
- CMake
- HTML/CSS/JavaScript (только UI)

## Структура проекта

- `web/include/` — C++ классы паттерна Декоратор для варианта с паттерном.
- `web/` — веб-версия с паттерном Декоратор.
- `web_without_pattern/` — веб-версия без применения паттерна.
- `Reports/Report_Decorator.md` — отчёт.

## Как запустить

### Вариант с паттерном (web)

Терминал 1 (C++ backend):

```bash
cd /Users/seanslinder/Study/OOAD/lab02/web
cmake -S . -B build
cmake --build build
./build/lab02_web_backend
```

Терминал 2 (frontend):

```bash
cd /Users/seanslinder/Study/OOAD/lab02/web
python3 -m http.server 8080
```

Открыть в браузере: `http://localhost:8080`

### Вариант без паттерна (web)

Терминал 1 (C++ backend):

```bash
cd /Users/seanslinder/Study/OOAD/lab02/web_without_pattern
cmake -S . -B build
cmake --build build
./build/lab02_web_backend
```

Терминал 2 (frontend):

```bash
cd /Users/seanslinder/Study/OOAD/lab02/web_without_pattern
python3 -m http.server 8090
```

Открыть в браузере: `http://localhost:8090`

## Как проявляется паттерн «Декоратор» (в `web/`)

1. `Pizza` задаёт общий интерфейс (`name()`, `cost()`).
2. `ItalianPizza` и `BulgarianPizza` — базовые объекты.
3. `PizzaDecorator` хранит ссылку на оборачиваемую пиццу.
4. `TomatoTopping`, `CheeseTopping`, `OlivesTopping`, `MushroomsTopping` добавляют поведение:

   - расширяют имя;
   - увеличивают цену.

5. В backend объект собирается динамически из выбранной базы и набора декораторов.
