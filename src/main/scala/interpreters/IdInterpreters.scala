package interpreters

import monads.Id
import algebras.*
import domain.{Train, Ticket}

// Реализация всех алгебр для Id.
// Id[A] = A, поэтому методы возвращают значения "здесь и сейчас", а эффекты
// выражаются через мутацию состояния в полях объектов-интерпретаторов.
// Это нормальная практика для Id-интерпретаторов в TF (см. одногруппника).
object IdInterpreters:

  // ---------- Console ----------
  given Console[Id] with
    def writeLine(s: String): Unit = println(s)
    def write(s: String): Unit     = print(s)
    def readLine: String =
      val s = scala.io.StdIn.readLine()
      if s == null then "" else s

  // ---------- Logger ----------
  class IdLogger extends Logger[Id]:
    private val buf = scala.collection.mutable.ArrayBuffer.empty[String]
    def add(line: String): Unit = buf += line
    def take: List[String] =
      val lines = buf.toList
      buf.clear()
      lines

  // ---------- IdSource ----------
  class IdIdSource(start: Int = 1) extends IdSource[Id]:
    private var counter: Int = start
    def nextTicketId: Int =
      val n = counter
      counter += 1
      n

  // ---------- OfficeOpen ----------
  class IdOfficeOpen(initiallyOpen: Boolean = true) extends OfficeOpen[Id]:
    private var openFlag: Boolean = initiallyOpen
    def isOpen: Boolean = openFlag
    def close: Unit     = openFlag = false
    def open: Unit      = openFlag = true

  // ---------- TrainRepo ----------
  class IdTrainRepo(initial: List[Train] = Nil) extends TrainRepo[Id]:
    private val storage = scala.collection.mutable.LinkedHashMap.empty[String, Train]
    initial.foreach(t => storage.update(t.name, t))

    def all: List[Train]                  = storage.values.toList
    def find(name: String): Option[Train] = storage.get(name)
    def add(train: Train): Unit           = storage.update(train.name, train)
    def setSeat(trainName: String, seat: String, occupied: Boolean): Unit =
      storage.get(trainName).foreach { t =>
        storage.update(trainName, t.copy(seats = t.seats.updated(seat, occupied)))
      }

  // ---------- TicketRepo ----------
  class IdTicketRepo extends TicketRepo[Id]:
    private val storage = scala.collection.mutable.LinkedHashMap.empty[Int, Ticket]
    def save(ticket: Ticket): Unit          = storage.update(ticket.id, ticket)
    def remove(ticketId: Int): Unit         = storage.remove(ticketId)
    def find(ticketId: Int): Option[Ticket] = storage.get(ticketId)
    def all: List[Ticket]                   = storage.values.toList
    def clear: Unit                         = storage.clear()

  // ---------- Revenue ----------
  class IdRevenue extends Revenue[Id]:
    private var amount: Double = 0.0
    def get: Double               = amount
    def add(d: Double): Unit      = amount += d
    def subtract(d: Double): Unit = amount -= d
    def reset: Unit               = amount = 0.0
