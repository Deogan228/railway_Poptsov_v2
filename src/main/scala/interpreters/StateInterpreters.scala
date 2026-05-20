package interpreters

import monads.{App, State, given}
import domain.{AppState, Train, Ticket}
import algebras.*

object StateInterpreters:

  given TrainRepo[App] with
    def all: App[List[Train]] =
      State.gets(_.trains.values.toList)
    def find(name: String): App[Option[Train]] =
      State.gets(_.trains.get(name))
    def add(train: Train): App[Unit] =
      State.modify(s => s.copy(trains = s.trains + (train.name -> train)))
    def setSeat(trainName: String, seat: String, occupied: Boolean): App[Unit] =
      State.modify { s =>
        s.trains.get(trainName).fold(s) { t =>
          val updated = t.copy(seats = t.seats.updated(seat, occupied))
          s.copy(trains = s.trains + (trainName -> updated))
        }
      }

  given TicketRepo[App] with
    def save(ticket: Ticket): App[Unit] =
      State.modify(s => s.copy(tickets = s.tickets + (ticket.id -> ticket)))
    def remove(id: Int): App[Unit] =
      State.modify(s => s.copy(tickets = s.tickets - id))
    def find(id: Int): App[Option[Ticket]] =
      State.gets(_.tickets.get(id))
    def all: App[List[Ticket]] =
      State.gets(_.tickets.values.toList)
    def clear: App[Unit] =
      State.modify(s => s.copy(tickets = Map.empty))

  given Revenue[App] with
    def get: App[Double] =
      State.gets(_.revenue)
    def add(amount: Double): App[Unit] =
      State.modify(s => s.copy(revenue = s.revenue + amount))
    def subtract(amount: Double): App[Unit] =
      State.modify(s => s.copy(revenue = s.revenue - amount))
    def reset: App[Unit] =
      State.modify(s => s.copy(revenue = 0.0))

  given IdSource[App] with
    def nextTicketId: App[Int] =
      State(s => (s.copy(nextId = s.nextId + 1), s.nextId))

  given OfficeOpen[App] with
    def isOpen: App[Boolean] =
      State.gets(_.isOpen)
    def close: App[Unit] =
      State.modify(s => s.copy(isOpen = false))
    def open: App[Unit] =
      State.modify(s => s.copy(isOpen = true))

  given Console[App] with
    def writeLine(s: String): App[Unit] =
      State(st => (st, println(s)))
    def write(s: String): App[Unit] =
      State(st => (st, print(s)))
    def readLine: App[String] =
      State { st =>
        val s = scala.io.StdIn.readLine()
        (st, if s == null then "" else s)
      }

  given Logger[App] with
    def add(line: String): App[Unit] =
      State(st => (st, ()))
    def take: App[List[String]] =
      State.gets(_ => List.empty)