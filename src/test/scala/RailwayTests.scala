import monads.{Id, given}
import domain.{TicketConfig, RouteTariff, Train, Ticket, ClassType, SeatRule, Pricing}
import algebras.*
import errors.{AppError, given}
import interpreters.IdInterpreters
import interpreters.IdInterpreters.given
import usecases.Booking

// Тесты бизнес-логики через Id-интерпретатор.
// Главная фишка: те же самые функции Booking.bookTicket / cancelTicket / ...
// можно проверить в чистом синхронном коде без всякого IO, без StdIn — просто
// вызовом и assert'ом на результат.
//
// запуск: sbt "Test / runMain RailwayTests"
object RailwayTests:
  def main(args: Array[String]): Unit =

    val cfg = TicketConfig(
      tariffs              = Map("Moscow-SPb" -> RouteTariff("Moscow-SPb", 2500, 5000)),
      baggagePerKg         = 50.0,
      seatRule             = SeatRule.Any,
      refundPenaltyPercent = 0.15
    )

    val seats = Map("1A" -> false, "1B" -> false, "2A" -> false, "2B" -> true)
    val train = Train("T1", "Moscow-SPb", seats)

    // ---------- 1. успешная бронь ----------
    {
      given TrainRepo[Id]   = IdInterpreters.IdTrainRepo(List(train))
      given TicketRepo[Id]  = IdInterpreters.IdTicketRepo()
      given Revenue[Id]     = IdInterpreters.IdRevenue()
      given IdSource[Id]    = IdInterpreters.IdIdSource(1)
      given OfficeOpen[Id]  = IdInterpreters.IdOfficeOpen(true)
      given Logger[Id]      = IdInterpreters.IdLogger()

      val res: Either[AppError, Ticket] =
        Booking.bookTicket[Id, AppError]("T1", "1A", ClassType.Economy, 10.0, cfg)

      assert(res.isRight, s"бронь должна пройти, получили $res")
      val ticket = res.toOption.get
      assert(ticket.price       == 2500, "цена эконома")
      assert(ticket.baggageCost == 500,  "багаж = 10 × 50")
      assert(ticket.trainName   == "T1", "trainName сохранён")
      assert(summon[Revenue[Id]].get == 3000.0, "выручка = 2500 + 500")
    }

    // ---------- 2. касса закрыта ----------
    {
      given TrainRepo[Id]   = IdInterpreters.IdTrainRepo(List(train))
      given TicketRepo[Id]  = IdInterpreters.IdTicketRepo()
      given Revenue[Id]     = IdInterpreters.IdRevenue()
      given IdSource[Id]    = IdInterpreters.IdIdSource(1)
      given OfficeOpen[Id]  = IdInterpreters.IdOfficeOpen(false)
      given Logger[Id]      = IdInterpreters.IdLogger()

      val res = Booking.bookTicket[Id, AppError]("T1", "1A", ClassType.Economy, 0, cfg)
      assert(res == Left(AppError.OfficeClosed), s"ожидался OfficeClosed, получили $res")
    }

    // ---------- 3. место занято ----------
    {
      given TrainRepo[Id]   = IdInterpreters.IdTrainRepo(List(train))
      given TicketRepo[Id]  = IdInterpreters.IdTicketRepo()
      given Revenue[Id]     = IdInterpreters.IdRevenue()
      given IdSource[Id]    = IdInterpreters.IdIdSource(1)
      given OfficeOpen[Id]  = IdInterpreters.IdOfficeOpen(true)
      given Logger[Id]      = IdInterpreters.IdLogger()

      val res = Booking.bookTicket[Id, AppError]("T1", "2B", ClassType.Economy, 0, cfg)
      assert(res == Left(AppError.SeatUnavailable("2B")), s"ожидался SeatUnavailable, получили $res")
    }

    // ---------- 4. поезда нет ----------
    {
      given TrainRepo[Id]   = IdInterpreters.IdTrainRepo(List(train))
      given TicketRepo[Id]  = IdInterpreters.IdTicketRepo()
      given Revenue[Id]     = IdInterpreters.IdRevenue()
      given IdSource[Id]    = IdInterpreters.IdIdSource(1)
      given OfficeOpen[Id]  = IdInterpreters.IdOfficeOpen(true)
      given Logger[Id]      = IdInterpreters.IdLogger()

      val res = Booking.bookTicket[Id, AppError]("T999", "1A", ClassType.Economy, 0, cfg)
      assert(res == Left(AppError.TrainNotFound("T999")), s"ожидался TrainNotFound, получили $res")
    }

    // ---------- 5. возврат билета + место освобождается только в нужном поезде ----------
    {
      val trainA = Train("T1", "Moscow-SPb", Map("1A" -> true))
      val trainB = Train("T2", "Moscow-SPb", Map("1A" -> true))
      val ticket = Ticket(1, "T1", "Moscow-SPb", ClassType.Economy, "1A", 2500, 0, 0)

      val trainsRepo  = IdInterpreters.IdTrainRepo(List(trainA, trainB))
      val ticketsRepo = IdInterpreters.IdTicketRepo()
      ticketsRepo.save(ticket)
      val revRepo = IdInterpreters.IdRevenue()
      revRepo.add(2500.0)

      given TrainRepo[Id]   = trainsRepo
      given TicketRepo[Id]  = ticketsRepo
      given Revenue[Id]     = revRepo
      given IdSource[Id]    = IdInterpreters.IdIdSource(1)
      given OfficeOpen[Id]  = IdInterpreters.IdOfficeOpen(true)
      given Logger[Id]      = IdInterpreters.IdLogger()

      val res = Booking.cancelTicket[Id, AppError](1, cfg)
      assert(res.isRight, s"отмена должна пройти, получили $res")

      val refunded = res.toOption.get
      assert(math.abs(refunded - 2125.0) < 0.01, s"возврат = 2500 × 0.85, получили $refunded")
      assert(trainsRepo.find("T1").get.seats("1A") == false, "место в T1 освободилось")
      assert(trainsRepo.find("T2").get.seats("1A") == true,  "место в T2 НЕ должно меняться")
      assert(ticketsRepo.find(1).isEmpty, "билет удалён")
    }

    // ---------- 6. Logger накапливает строки ----------
    {
      given TrainRepo[Id]    = IdInterpreters.IdTrainRepo(List(train))
      given TicketRepo[Id]   = IdInterpreters.IdTicketRepo()
      given Revenue[Id]      = IdInterpreters.IdRevenue()
      given IdSource[Id]     = IdInterpreters.IdIdSource(1)
      given OfficeOpen[Id]   = IdInterpreters.IdOfficeOpen(true)
      val logger             = IdInterpreters.IdLogger()
      given Logger[Id]       = logger

      val _ = Booking.bookTicket[Id, AppError]("T1", "1A", ClassType.Economy, 5.0, cfg)
      val lines = logger.take
      assert(lines.exists(_.contains("Тариф")),    "в логе тариф")
      assert(lines.exists(_.contains("Багаж")),    "в логе багаж")
      assert(lines.exists(_.contains("свободно")), "в логе seatAvailable")
      assert(lines.exists(_.contains("оформлен")), "в логе оформление")
    }

    // ---------- 7. nextDay сбрасывает билеты и выручку, открывает кассу ----------
    {
      val ticketsRepo = IdInterpreters.IdTicketRepo()
      ticketsRepo.save(Ticket(1, "T1", "Moscow-SPb", ClassType.Economy, "1A", 2500, 0, 0))
      val revRepo = IdInterpreters.IdRevenue()
      revRepo.add(2500.0)
      val officeRepo = IdInterpreters.IdOfficeOpen(false)

      given TrainRepo[Id]   = IdInterpreters.IdTrainRepo(List(train))
      given TicketRepo[Id]  = ticketsRepo
      given Revenue[Id]     = revRepo
      given IdSource[Id]    = IdInterpreters.IdIdSource(1)
      given OfficeOpen[Id]  = officeRepo
      given Logger[Id]      = IdInterpreters.IdLogger()

      Booking.nextDay[Id]
      assert(ticketsRepo.all.isEmpty, "билеты сброшены")
      assert(revRepo.get == 0.0,      "выручка обнулена")
      assert(officeRepo.isOpen,       "касса открыта в новый день")
    }

    // ---------- 8. чистые функции Pricing (без F) ----------
    {
      val priceR = Pricing.ticketPrice(cfg, "Moscow-SPb", ClassType.Business)
      assert(priceR.value.contains(5000), "бизнес = 5000")

      val bagR = Pricing.baggageCost(cfg, 5)
      assert(bagR.value == 250, "багаж = 5 × 50")

      val seatR = Pricing.seatAvailable(cfg, train, "1A")
      assert(seatR.value, "1A свободно")

      val cfgWin = cfg.copy(seatRule = SeatRule.Window)
      assert( Pricing.seatAvailable(cfgWin, train, "1A").value, "window: 1A разрешено")
      assert(!Pricing.seatAvailable(cfgWin, train, "1B").value, "window: 1B запрещено")

      val cfgAisle = cfg.copy(seatRule = SeatRule.Aisle)
      assert(!Pricing.seatAvailable(cfgAisle, train, "1A").value, "aisle: 1A запрещено")
      assert( Pricing.seatAvailable(cfgAisle, train, "1B").value, "aisle: 1B разрешено")
    }

    // ---------- 9. добавление поезда ----------
    {
      val repo = IdInterpreters.IdTrainRepo(List(train))
      given TrainRepo[Id] = repo
      given Logger[Id]    = IdInterpreters.IdLogger()

      val res1 = Booking.addTrain[Id, AppError](Train("T2", "SPb-Sochi", Map("1A" -> false)))
      assert(res1.isRight, "новый поезд добавляется")
      assert(repo.all.size == 2, "теперь 2 поезда")

      val res2 = Booking.addTrain[Id, AppError](Train("T1", "Anywhere", Map.empty))
      assert(res2 == Left(AppError.TrainAlreadyExists("T1")), s"дубль не добавляется, получили $res2")
    }

    println("все TF-тесты пройдены")
