import monads.{Id, given}
import domain.{TicketConfig, RouteTariff, Train, SeatRule}
import algebras.*
import interpreters.IdInterpreters
import interpreters.IdInterpreters.given
import tf.Program

// Точка сборки приложения.
// Здесь выбирается конкретный F (Id) и создаются все интерпретаторы алгебр.
// Program.run полностью не знает что именно за F — это и есть Tagless Final.
@main def main(): Unit =
  val cfg = TicketConfig(
    tariffs = Map(
      "Moscow-SPb"   -> RouteTariff("Moscow-SPb",   2500, 5000),
      "Moscow-Kazan" -> RouteTariff("Moscow-Kazan", 1800, 3600),
      "SPb-Sochi"    -> RouteTariff("SPb-Sochi",    3200, 6400)
    ),
    baggagePerKg         = 50.0,
    seatRule             = SeatRule.Any,
    refundPenaltyPercent = 0.15
  )

  // интерпретаторы с начальным состоянием.
  // часть given'ов уже определена в IdInterpreters (Console),
  // часть создаём здесь с начальными данными.
  given Logger[Id]      = IdInterpreters.IdLogger()
  given IdSource[Id]    = IdInterpreters.IdIdSource(1)
  given OfficeOpen[Id]  = IdInterpreters.IdOfficeOpen(true)
  given TicketRepo[Id]  = IdInterpreters.IdTicketRepo()
  given Revenue[Id]     = IdInterpreters.IdRevenue()
  given TrainRepo[Id]   = IdInterpreters.IdTrainRepo(List(
    Train("Express-1", "Moscow-SPb",   Train.makeSeats(3)),
    Train("Express-2", "Moscow-Kazan", Train.makeSeats(3))
  ))

  // Program.run[Id] возвращает Id[Unit] = Unit, выполняется синхронно
  Program.run[Id](cfg)
