package domain

object Pricing:

  // результат расчёта + накопленный лог
  case class Logged[A](value: A, log: List[String])

  // ищем тариф по маршруту
  def ticketPrice(cfg: TicketConfig, route: String, classType: ClassType): Logged[Option[Double]] =
    cfg.tariffs.get(route) match
      case None => //если не нашли
        Logged(None, List(s"Тариф для маршрута $route не найден"))
      case Some(t) => //если нашли, берем нужную цену
        val price = classType match
          case ClassType.Economy  => t.economy
          case ClassType.Business => t.business
        Logged(Some(price), List(s"Тариф $route ($classType): $price"))

  // стоимость багажа
  def baggageCost(cfg: TicketConfig, weight: Double): Logged[Double] =
    if weight <= 0 then Logged(0.0, List("Багаж: нет"))
    else
      val cost = weight * cfg.baggagePerKg
      Logged(cost, List(s"Багаж: $weight кг × ${cfg.baggagePerKg} = $cost"))

  // проверка места: учитывает занятость и SeatRule из конфига
  def seatAvailable(cfg: TicketConfig, train: Train, seat: String): Logged[Boolean] =
    train.seats.get(seat) match
      case None =>
        Logged(false, List(s"Места $seat нет в поезде ${train.name}"))
      case Some(true) =>
        Logged(false, List(s"Место $seat занято"))
      case Some(false) =>
        val allowed = cfg.seatRule match
          case SeatRule.Any    => true
          case SeatRule.Window => train.isWindow(seat)
          case SeatRule.Aisle  => train.isAisle(seat)
        if allowed then
          Logged(true,  List(s"Место $seat свободно (правило ${cfg.seatRule})"))
        else
          Logged(false, List(s"Место $seat не подходит по правилу ${cfg.seatRule}"))

  // сумма возврата с учётом штрафа
  def refundAmount(cfg: TicketConfig, ticket: Ticket): Logged[Double] =
    val total  = ticket.price + ticket.baggageCost
    val refund = total * (1.0 - cfg.refundPenaltyPercent)
    Logged(
      refund,
      List(
        s"Билет #${ticket.id}: цена=${ticket.price} багаж=${ticket.baggageCost} итого=$total",
        s"Штраф ${cfg.refundPenaltyPercent * 100}%, возврат=$refund"
      )
    )
