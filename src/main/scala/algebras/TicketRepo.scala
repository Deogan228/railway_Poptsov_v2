package algebras

import domain.Ticket

// репозиторий проданных билетов
trait TicketRepo[F[_]]:
  def save(ticket: Ticket): F[Unit] //cохранить проданный билет
  def remove(ticketId: Int): F[Unit] //удалить по id
  def find(ticketId: Int): F[Option[Ticket]] //найти по id
  def all: F[List[Ticket]] //все билеты
  def clear: F[Unit] //очистить все
