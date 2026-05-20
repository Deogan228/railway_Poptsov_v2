package algebras

// генератор последовательных id для билетов
trait IdSource[F[_]]:
  def nextTicketId: F[Int]
