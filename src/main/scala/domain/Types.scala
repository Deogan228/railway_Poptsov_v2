package domain

enum ClassType:
  case Economy, Business

case class RouteTariff(
  route: String,
  economy: Double,
  business: Double
)

// правило выбора места: window — только места A/D (у окна),
// aisle — только B/C (у прохода), any — любое
enum SeatRule:
  case Window, Aisle, Any

// конфиг кассы — передаётся как обычный параметр в use-case,
// без Reader-монады (TF не требует Reader, конфиг достаточно передать руками)
case class TicketConfig(
  tariffs: Map[String, RouteTariff],
  baggagePerKg: Double,
  seatRule: SeatRule,
  refundPenaltyPercent: Double         // доля штрафа, напр. 0.15 = 15%
)

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

// seats: место -> занято (true) или свободно (false)
case class Train(
  name: String,
  route: String,
  seats: Map[String, Boolean]
):
  // место у окна: последний символ A или D, у прохода: B или C
  def isWindow(seat: String): Boolean =
    seat.nonEmpty && (seat.last == 'A' || seat.last == 'D')
  def isAisle(seat: String): Boolean =
    seat.nonEmpty && (seat.last == 'B' || seat.last == 'C')

object Train:
  // создание карты мест: nRows рядов по 4 места (A, B, C, D)
  def makeSeats(nRows: Int): Map[String, Boolean] =
    val pairs = for
      r <- 1 to nRows
      c <- Seq("A", "B", "C", "D")
    yield s"$r$c" -> false
    pairs.toMap

// OfficeState больше нет — состояние разнесено по алгебрам:
//   trains        → TrainRepo[F]
//   soldTickets   → TicketRepo[F]
//   revenue       → Revenue[F]
//   nextTicketId  → IdSource[F]
//   isOpen (новое) → OfficeOpen[F]
