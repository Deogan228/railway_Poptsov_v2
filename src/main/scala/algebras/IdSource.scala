package algebras

// генератор последовательных id (для номеров билетов)
trait IdSource[F[_]]:
  def nextTicketId: F[Int]
