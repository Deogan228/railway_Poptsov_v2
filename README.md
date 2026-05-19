# Вариант 13. Железнодорожная касса (Tagless Final)

Касса продаёт билеты, учитывает багаж и свободные места.
Реализация в стиле **Tagless Final**: бизнес-логика обобщена над `F[_]: Monad`,
эффекты вынесены в алгебры, конкретная монада подставляется в точке сборки.

## Запуск

```
sbt run
```

Для русских символов в консоли Windows предварительно:
```
chcp 65001
```

## Тесты

```
sbt "Test / runMain RailwayTests"
```

## Управление

- Вводите номер пункта меню и нажмите Enter.
- `0` — возврат / выход.

## Структура проекта

    src/main/scala/
        ├── monads/           — Monad, Id
        ├── domain/           — Types (типы), Pricing (чистые функции тарификации)
        ├── algebras/         — 7 TF-алгебр
        │     ├── Console        — ввод/вывод
        │     ├── Logger         — накопитель лога
        │     ├── IdSource       — номера билетов
        │     ├── TrainRepo      — поезда и места
        │     ├── TicketRepo     — проданные билеты
        │     ├── Revenue        — выручка
        │     └── OfficeOpen     — статус кассы (открыта/закрыта)
        ├── errors/           — type-class ошибки + AppError
        ├── usecases/         — Booking: bookTicket / cancelTicket / addTrain / nextDay / closeOffice
        ├── interpreters/     — IdInterpreters: реализация алгебр на var/mutable
        ├── tf/               — Menu, Program (сценарии меню обобщённые над F)
        └── main.scala        — точка сборки приложения (Id-интерпретатор)

## Ключевая идея

Use-case в `usecases/Booking.scala` написан как:

```scala
def bookTicket[F[_]: Monad, E](...)(using
  trains: TrainRepo[F], tickets: TicketRepo[F], revenue: Revenue[F],
  ids: IdSource[F], office: OfficeOpen[F], log: Logger[F],
  closedErr: OfficeClosed[E], notFoundErr: TrainNotFound[E],
  seatErr: SeatUnavailable[E], tariffErr: NoTariff[E]
): F[Either[E, Ticket]]
```

Функция ничего не знает про конкретный F (Id, IO, Future, ZIO…), про конкретный E
(AppError или что-то ещё), про конкретные реализации алгебр. Точка сборки
(`main.scala`) выбирает F = Id, подключает интерпретаторы и конкретный AppError —
никакого изменения кода use-case'а это не требует.

## Почему такая декомпозиция

**Алгебры вместо монад.** Раньше в проекте были IO, Reader, Writer, State.
В TF от них всех ушли:

- IO → `Console[F]` (плюс выбор интерпретатора в `main.scala`).
- State → разнесён по алгебрам данных: `TrainRepo`, `TicketRepo`, `Revenue`,
  `IdSource`, `OfficeOpen`. Это естественно: разные части состояния редко
  читаются вместе, единый `State[OfficeState, _]` склеивал их искусственно.
- Reader → `TicketConfig` передаётся обычным параметром в use-case. Reader
  нужен когда контекст глубоко прокидывается через много слоёв; в TF у нас
  ровно один use-case-слой, отдельная монада чтения только мешает.
- Writer → `Logger[F]` + чистые функции `Pricing` возвращают `Logged[A]` с
  списком строк. Сбор лога теперь явный, а не "магически копится в монаде".

**Type-class ошибок.** Use-case требует только что для `E` определены нужные
конструкторы (`TrainNotFound[E]`, `SeatUnavailable[E]` и т.д.). Конкретный
`enum AppError` подключается через `given` в `errors/AppError.scala`.
Это позволяет иметь несколько вариантов представления ошибок (например, более
детальный для логирования и более простой для UI) без переписывания логики.

**Меню как Composite над F.** `MenuLeaf` / `MenuTreeNode` параметризованы по F,
поддерево — то же `MenuOption[F]`. Добавить новый пункт = одна строка в
`Program.run`. Добавить подменю = одна вложенная нода.

## Что осталось от изначальной лабораторной

- Структура домена (`Train`, `Ticket`, `RouteTariff`, `ClassType`, `SeatRule`).
- Логика тарификации (теперь чистые функции в `domain/Pricing.scala`).
- Тип-класс `Monad[F]` с extension-методами для for-comprehension.
- Composite-меню (теперь над `F[_]`).

## Что появилось нового

- 7 алгебр и Id-интерпретаторы для всех них.
- Алгебра ошибок (type-class).
- `OfficeOpen` — касса может быть закрыта на ночь.
- Параметризация всех операций по `F[_]: Monad`.
