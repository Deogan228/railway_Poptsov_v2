import monads.{App, given}
import domain.{TicketConfig, RouteTariff, Train, SeatRule}
import domain.AppState
import interpreters.StateInterpreters.given
import algebras.*
import tf.Program

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

  val initState = AppState(
    trains = Map(
      "Express-1" -> Train("Express-1", "Moscow-SPb",   Train.makeSeats(3)),
      "Express-2" -> Train("Express-2", "Moscow-Kazan", Train.makeSeats(3))
    )
  )

  val (finalState, _) = Program.run[App](cfg).run(initState)
