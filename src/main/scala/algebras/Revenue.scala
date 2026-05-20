package algebras

// выручка кассы
trait Revenue[F[_]]:
  def get: F[Double] //текущая выручка
  def add(amount: Double): F[Unit] //добавить при продаже
  def subtract(amount: Double): F[Unit] //вычесть при возврате
  def reset: F[Unit] //сбросить выручку в 0
