package algebras

trait IdSource[F[_]]:
  def nextTicketId: F[Int]
