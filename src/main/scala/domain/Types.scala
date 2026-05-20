package domain

enum ClassType:
  case Economy, Business

case class RouteTariff(
  route: String,
  economy: Double,
  business: Double
)

enum SeatRule:
  case Window, Aisle, Any

case class TicketConfig(
  tariffs: Map[String, RouteTariff],
  baggagePerKg: Double, 
  seatRule: SeatRule, 
  refundPenaltyPercent: Double 
)

//проданный билет 
case class Ticket(
  id: Int,
  trainName: String,
  route: String,
  classType: ClassType,
  seat: String,
  price: Double,
  baggageWeight: Double,
  baggageCost: Double
)

// поезд
case class Train(
  name: String,
  route: String,
  seats: Map[String, Boolean]
):
  def isWindow(seat: String): Boolean =
    seat.nonEmpty && (seat.last == 'A' || seat.last == 'D')
  def isAisle(seat: String): Boolean =
    seat.nonEmpty && (seat.last == 'B' || seat.last == 'C')

object Train:
  def makeSeats(nRows: Int): Map[String, Boolean] =
    val pairs = for
      r <- 1 to nRows
      c <- Seq("A", "B", "C", "D")
    yield s"$r$c" -> false
    pairs.toMap