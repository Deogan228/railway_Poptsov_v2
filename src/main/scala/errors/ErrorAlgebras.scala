package errors

trait TrainNotFound[E]:
  def trainNotFound(name: String): E

trait SeatUnavailable[E]:
  def seatUnavailable(seat: String): E

trait NoTariff[E]:
  def noTariff(route: String): E

trait TicketNotFound[E]:
  def ticketNotFound(id: Int): E

trait OfficeClosed[E]:
  def officeClosed: E

trait TrainAlreadyExists[E]:
  def trainAlreadyExists(name: String): E
