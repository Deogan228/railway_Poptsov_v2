package algebras

// состояние кассы: открыта/закрыта.
// закрытая касса не продаёт билеты и не оформляет возвраты.
// аналог ExamControl у одногруппника.
trait OfficeOpen[F[_]]:
  def isOpen: F[Boolean]
  def close: F[Unit]
  def open: F[Unit]
