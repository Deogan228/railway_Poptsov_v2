package tf

import monads.{*, given}
import domain.{TicketConfig, ClassType, Train}
import algebras.*
import errors.*
import errors.given
import usecases.Booking

// сценарии действий меню в TF-стиле.
// дефолты для пользовательского ввода и сообщения - именованные константы вместо магии.
object Program:

  // дефолты для пользовательского ввода
  object Defaults:
    val NoBaggageWeight   = 0.0
    val DefaultTrainRows  = 10

  // ----- ввод -----

  private def readIntOpt[F[_]: Monad](prompt: String)(using c: Console[F]): F[Option[Int]] =
    for
      _ <- c.write(prompt)
      s <- c.readLine
    yield s.trim.toIntOption

  private def readIntOr[F[_]: Monad](prompt: String, fallback: Int)(using c: Console[F]): F[Int] =
    readIntOpt(prompt).map(_.getOrElse(fallback))

  private def readDouble[F[_]: Monad](prompt: String, fallback: Double)(using c: Console[F]): F[Double] =
    for
      _ <- c.write(prompt)
      s <- c.readLine
    yield s.trim.toDoubleOption.getOrElse(fallback)

  private def readStr[F[_]: Monad](prompt: String)(using c: Console[F]): F[String] =
    for
      _ <- c.write(prompt)
      s <- c.readLine
    yield s.trim

  private def parseClassType(input: String): ClassType =
    input.trim.toLowerCase match
      case "business" | "b" => ClassType.Business
      case _                => ClassType.Economy

  // печать накопленных в Logger строк
  private def flushAndPrint[F[_]: Monad](using c: Console[F], log: Logger[F]): F[Unit] =
    log.take.flatMap { lines =>
      if lines.isEmpty then summon[Monad[F]].pure(())
      else c.writeLine(lines.map("  " + _).mkString("\n"))
    }

  // ----- сценарии -----

  def showTrainsScenario[F[_]: Monad](using c: Console[F], trains: TrainRepo[F]): F[Unit] =
    trains.all.flatMap { ts =>
      if ts.isEmpty then c.writeLine("  поездов нет")
      else
        val lines = ts.zipWithIndex.map { case (t, i) =>
          val free = t.seats.count { case (_, occ) => !occ }
          s"  ${i + 1}. ${t.name} | ${t.route} | свободно: $free/${t.seats.size}"
        }
        c.writeLine(lines.mkString("\n"))
    }

  def showTicketsScenario[F[_]: Monad](using
      c: Console[F], tickets: TicketRepo[F]
  ): F[Unit] =
    tickets.all.flatMap { ts =>
      if ts.isEmpty then c.writeLine("  билетов нет")
      else
        val lines = ts.map { t =>
          s"  #${t.id} ${t.trainName} ${t.route} ${t.classType} место=${t.seat} цена=${t.price} багаж=${t.baggageCost}"
        }
        c.writeLine(lines.mkString("\n"))
    }

  def bookScenario[F[_]: Monad](cfg: TicketConfig)(using
      c: Console[F],
      trains: TrainRepo[F],
      tickets: TicketRepo[F],
      revenue: Revenue[F],
      ids: IdSource[F],
      office: OfficeOpen[F],
      log: Logger[F]
  ): F[Unit] =
    for
      _           <- showTrainsScenario[F]
      trainNumOpt <- readIntOpt[F]("Номер поезда: ")
      ts          <- trains.all
      train        = trainNumOpt.flatMap(n => ts.lift(n - 1))
      _           <- train match
        case None =>
          c.writeLine("  ошибка: поезд не выбран")
        case Some(t) =>
          val free = t.seats.filter { case (_, o) => !o }.keys.toList.sorted
          for
            _       <- c.writeLine(s"Свободные места: ${free.mkString(", ")}")
            seat    <- readStr[F]("Место: ")
            clsStr  <- readStr[F]("Класс (economy/business): ")
            cls      = parseClassType(clsStr)
            baggage <- readDouble[F]("Вес багажа кг (0 если нет): ", Defaults.NoBaggageWeight)
            res     <- Booking.bookTicket[F, AppError](t.name, seat, cls, baggage, cfg)
            _       <- flushAndPrint[F]
            _       <- res match
              case Right(ticket) => c.writeLine(s"  => билет #${ticket.id} оформлен")
              case Left(e)       => c.writeLine(s"  ошибка: ${render(e)}")
          yield ()
    yield ()

  def cancelScenario[F[_]: Monad](cfg: TicketConfig)(using
      c: Console[F],
      trains: TrainRepo[F],
      tickets: TicketRepo[F],
      revenue: Revenue[F],
      office: OfficeOpen[F],
      log: Logger[F]
  ): F[Unit] =
    for
      _     <- showTicketsScenario[F]
      idOpt <- readIntOpt[F]("Номер билета: ")
      _ <- idOpt match
        case None => c.writeLine("  ошибка: номер билета не указан")
        case Some(id) =>
          for
            res <- Booking.cancelTicket[F, AppError](id, cfg)
            _   <- flushAndPrint[F]
            _   <- res match
              case Right(amount) => c.writeLine(s"  => возврат $amount")
              case Left(e)       => c.writeLine(s"  ошибка: ${render(e)}")
          yield ()
    yield ()

  def addTrainScenario[F[_]: Monad](using
      c: Console[F],
      trains: TrainRepo[F],
      log: Logger[F]
  ): F[Unit] =
    for
      name  <- readStr[F]("Название поезда: ")
      route <- readStr[F]("Маршрут (напр. Moscow-SPb): ")
      rows  <- readIntOr[F]("Количество рядов: ", Defaults.DefaultTrainRows)
      train  = Train(name, route, Train.makeSeats(rows))
      res   <- Booking.addTrain[F, AppError](train)
      _     <- flushAndPrint[F]
      _     <- res match
        case Right(_) => c.writeLine(s"  => поезд $name добавлен")
        case Left(e)  => c.writeLine(s"  ошибка: ${render(e)}")
    yield ()

  def nextDayScenario[F[_]: Monad](using
      c: Console[F],
      tickets: TicketRepo[F],
      revenue: Revenue[F],
      office: OfficeOpen[F],
      log: Logger[F]
  ): F[Unit] =
    for
      _ <- Booking.nextDay[F]
      _ <- flushAndPrint[F]
    yield ()

  def closeScenario[F[_]: Monad](using
      c: Console[F],
      office: OfficeOpen[F],
      log: Logger[F]
  ): F[Unit] =
    for
      _ <- Booking.closeOffice[F]
      _ <- flushAndPrint[F]
    yield ()

  // ----- сборка приложения -----
  def run[F[_]: Monad](cfg: TicketConfig)(using
      c: Console[F],
      trains: TrainRepo[F],
      tickets: TicketRepo[F],
      revenue: Revenue[F],
      ids: IdSource[F],
      office: OfficeOpen[F],
      log: Logger[F]
  ): F[Unit] =
    val root = MenuTreeNode[F](
      "Железнодорожная касса (TF)",
      Seq(
        MenuLeaf("Показать поезда",   () => showTrainsScenario[F]),
        MenuLeaf("Показать билеты",   () => showTicketsScenario[F]),
        MenuLeaf("Купить билет",      () => bookScenario[F](cfg)),
        MenuLeaf("Вернуть билет",     () => cancelScenario[F](cfg)),
        MenuLeaf("Добавить поезд",    () => addTrainScenario[F]),
        MenuTreeNode("Касса", Seq(
          MenuLeaf("Закрыть кассу",   () => closeScenario[F]),
          MenuLeaf("Следующий день",  () => nextDayScenario[F])
        ))
      )
    )

    // динамический суффикс заголовка: статус кассы + выручка
    def suffix: F[String] =
      for
        op  <- office.isOpen
        rev <- revenue.get
      yield s" [${if op then "открыта" else "закрыта"}, выручка=$rev]"

    for
      _ <- c.writeLine("=== Железнодорожная касса (TF / монады) ===")
      _ <- c.writeLine(s"Маршруты: ${cfg.tariffs.keys.mkString(", ")}")
      _ <- c.writeLine(s"Багаж: ${cfg.baggagePerKg} руб/кг, штраф за возврат: ${cfg.refundPenaltyPercent * 100}%")
      _ <- c.writeLine(s"Правило выбора места: ${cfg.seatRule}")
      _ <- Menu.loop(root, () => suffix)
      _ <- c.writeLine("пока")
    yield ()
