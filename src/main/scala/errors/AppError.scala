package errors

// конкретная реализация ошибок: enum + given-инстансы алгебр выше

enum AppError:
  case TrainNotFound(name: String)
  case SeatUnavailable(seat: String)
  case NoTariff(route: String)
  case TicketNotFound(id: Int)
  case OfficeClosed
  case TrainAlreadyExists(name: String)

given TrainNotFound[AppError] with
  def trainNotFound(name: String): AppError = AppError.TrainNotFound(name)

given SeatUnavailable[AppError] with
  def seatUnavailable(seat: String): AppError = AppError.SeatUnavailable(seat)

given NoTariff[AppError] with
  def noTariff(route: String): AppError = AppError.NoTariff(route)

given TicketNotFound[AppError] with
  def ticketNotFound(id: Int): AppError = AppError.TicketNotFound(id)

given OfficeClosed[AppError] with
  def officeClosed: AppError = AppError.OfficeClosed

given TrainAlreadyExists[AppError] with
  def trainAlreadyExists(name: String): AppError = AppError.TrainAlreadyExists(name)

// рендеринг ошибки в строку (для UI)
def render(e: AppError): String = e match
  case AppError.TrainNotFound(name)      => s"поезд $name не найден"
  case AppError.SeatUnavailable(seat)    => s"место $seat недоступно"
  case AppError.NoTariff(route)          => s"нет тарифа для маршрута $route"
  case AppError.TicketNotFound(id)       => s"билет #$id не найден"
  case AppError.OfficeClosed             => "касса закрыта"
  case AppError.TrainAlreadyExists(name) => s"поезд $name уже существует"
