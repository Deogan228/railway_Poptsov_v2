package domain

case class AppState(
  trains:  Map[String, Train]  = Map.empty,
  tickets: Map[Int, Ticket]    = Map.empty,
  revenue: Double              = 0.0,
  nextId:  Int                 = 1,
  isOpen:  Boolean             = true
)