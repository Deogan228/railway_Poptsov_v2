import monads.{Id, given}
import domain.{TicketConfig, RouteTariff, Train, SeatRule}
import algebras.*
import interpreters.IdInterpreters
import interpreters.IdInterpreters.given
import tf.Program

//создаем конфигурацию
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

  // создаем алгебры и интерпретаторы, теперь компилятор знает, как работать с Id
  given Logger[Id]      = IdInterpreters.IdLogger()
  given IdSource[Id]    = IdInterpreters.IdIdSource(1)
  given OfficeOpen[Id]  = IdInterpreters.IdOfficeOpen(true)
  given TicketRepo[Id]  = IdInterpreters.IdTicketRepo()
  given Revenue[Id]     = IdInterpreters.IdRevenue()
  given TrainRepo[Id]   = IdInterpreters.IdTrainRepo(List(
    Train("Express-1", "Moscow-SPb",   Train.makeSeats(3)),
    Train("Express-2", "Moscow-Kazan", Train.makeSeats(3))
  ))

  // запуск
  Program.run[Id](cfg)
