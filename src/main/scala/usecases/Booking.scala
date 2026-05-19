package usecases

import monads.{Monad, given}
import domain.{TicketConfig, ClassType, Train, Ticket, Pricing}
import algebras.*
import errors.*

// Главная идея TF: бизнес-логика написана как функция в любой F с подходящими алгебрами.
// Никакого IO, никакого State, никакого Reader — только F[_]: Monad и набор using-алгебр.
// Какой F использовать (Id, IO, Future, ZIO...) решает точка сборки приложения.
object Booking:

  // вспомогательная функция: вылить накопленный лог чистых функций (Pricing.Logged)
  // в Logger[F]
  private def flushLog[F[_]: Monad](lines: List[String])(using logger: Logger[F]): F[Unit] =
    val F = summon[Monad[F]]
    lines.foldLeft(F.pure(()))((acc, line) => acc.flatMap(_ => logger.add(line)))

  // ============ бронь билета ============
  // 4 возможных ошибки: касса закрыта, поезда нет, место недоступно, нет тарифа.
  // E (тип ошибки) — параметр. use-case требует только что для E определены нужные
  // конструкторы. конкретный AppError подставится в точке сборки.
  def bookTicket[F[_]: Monad, E](
      trainName: String,
      seat: String,
      classType: ClassType,
      baggageWeight: Double,
      cfg: TicketConfig
  )(using
      trains: TrainRepo[F],
      tickets: TicketRepo[F],
      revenue: Revenue[F],
      ids: IdSource[F],
      office: OfficeOpen[F],
      log: Logger[F],
      closedErr: OfficeClosed[E],
      notFoundErr: TrainNotFound[E],
      seatErr: SeatUnavailable[E],
      tariffErr: NoTariff[E]
  ): F[Either[E, Ticket]] =
    val F = summon[Monad[F]]

    // явная аннотация типа — помогает inferer'у Scala 3 не путаться с Either
    type R = Either[E, Ticket]

    office.isOpen.flatMap { open =>
      if !open then F.pure[R](Left(closedErr.officeClosed))
      else trains.find(trainName).flatMap {
        case None =>
          log.add(s"Ошибка: поезд $trainName не найден").flatMap(_ =>
            F.pure[R](Left(notFoundErr.trainNotFound(trainName))))
        case Some(train) =>
          // чистые проверки из Pricing
          val seatRes  = Pricing.seatAvailable(cfg, train, seat)
          val priceRes = Pricing.ticketPrice(cfg, train.route, classType)
          val bagRes   = Pricing.baggageCost(cfg, baggageWeight)
          val allLog   = seatRes.log ++ priceRes.log ++ bagRes.log

          flushLog(allLog).flatMap { _ =>
            if !seatRes.value then
              F.pure[R](Left(seatErr.seatUnavailable(seat)))
            else priceRes.value match
              case None        => F.pure[R](Left(tariffErr.noTariff(train.route)))
              case Some(price) =>
                val ticket = Ticket(
                  id            = 0, // подменим ниже
                  trainName     = train.name,
                  route         = train.route,
                  classType     = classType,
                  seat          = seat,
                  price         = price,
                  baggageWeight = baggageWeight,
                  baggageCost   = bagRes.value
                )
                for
                  id <- ids.nextTicketId
                  t   = ticket.copy(id = id)
                  _  <- trains.setSeat(train.name, seat, true)
                  _  <- tickets.save(t)
                  _  <- revenue.add(price + bagRes.value)
                  _  <- log.add(s"Билет #$id оформлен: $trainName место=$seat класс=$classType цена=$price багаж=${bagRes.value}")
                yield (Right(t): R)
          }
      }
    }

  // ============ отмена билета ============
  def cancelTicket[F[_]: Monad, E](ticketId: Int, cfg: TicketConfig)(using
      tickets: TicketRepo[F],
      trains: TrainRepo[F],
      revenue: Revenue[F],
      office: OfficeOpen[F],
      log: Logger[F],
      closedErr: OfficeClosed[E],
      notFoundErr: TicketNotFound[E]
  ): F[Either[E, Double]] =
    val F = summon[Monad[F]]
    type R = Either[E, Double]

    office.isOpen.flatMap { open =>
      if !open then F.pure[R](Left(closedErr.officeClosed))
      else tickets.find(ticketId).flatMap {
        case None         => F.pure[R](Left(notFoundErr.ticketNotFound(ticketId)))
        case Some(ticket) =>
          val refundRes = Pricing.refundAmount(cfg, ticket)
          for
            _ <- flushLog(refundRes.log)
            _ <- trains.setSeat(ticket.trainName, ticket.seat, false)
            _ <- tickets.remove(ticketId)
            _ <- revenue.subtract(refundRes.value)
            _ <- log.add(s"Билет #$ticketId отменён, возврат=${refundRes.value}")
          yield (Right(refundRes.value): R)
      }
    }

  // ============ добавление поезда ============
  def addTrain[F[_]: Monad, E](train: Train)(using
      trains: TrainRepo[F],
      log: Logger[F],
      existsErr: TrainAlreadyExists[E]
  ): F[Either[E, Unit]] =
    val F = summon[Monad[F]]
    type R = Either[E, Unit]
    trains.find(train.name).flatMap {
      case Some(_) => F.pure[R](Left(existsErr.trainAlreadyExists(train.name)))
      case None    =>
        for
          _ <- trains.add(train)
          _ <- log.add(s"Поезд ${train.name} добавлен, маршрут=${train.route}, мест=${train.seats.size}")
        yield (Right(()): R)
    }

  // ============ новый день ============
  // сброс билетов и выручки, касса снова открыта
  def nextDay[F[_]: Monad](using
      tickets: TicketRepo[F],
      revenue: Revenue[F],
      office: OfficeOpen[F],
      log: Logger[F]
  ): F[Unit] =
    for
      prev <- revenue.get
      _    <- tickets.clear
      _    <- revenue.reset
      _    <- office.open
      _    <- log.add(s"Новый день. Выручка за вчера: $prev, билеты сброшены.")
    yield ()

  // ============ закрытие кассы ============
  def closeOffice[F[_]: Monad](using
      office: OfficeOpen[F],
      log: Logger[F]
  ): F[Unit] =
    for
      _ <- office.close
      _ <- log.add("Касса закрыта")
    yield ()
