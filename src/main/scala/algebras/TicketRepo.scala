package algebras

import domain.Ticket

trait TicketRepo[F[_]]:
  def save(ticket: Ticket): F[Unit]
  def remove(ticketId: Int): F[Unit] 
  def find(ticketId: Int): F[Option[Ticket]] 
  def all: F[List[Ticket]] 
  def clear: F[Unit] 
