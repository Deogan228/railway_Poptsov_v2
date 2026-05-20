package algebras

trait OfficeOpen[F[_]]:
  def isOpen: F[Boolean] 
  def close: F[Unit] 
  def open: F[Unit] 
