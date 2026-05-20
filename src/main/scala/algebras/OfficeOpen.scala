package algebras

// состояние кассы
trait OfficeOpen[F[_]]:
  def isOpen: F[Boolean] //открыта ли
  def close: F[Unit] //закрыть
  def open: F[Unit] //открыть
