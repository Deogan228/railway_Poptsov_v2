package algebras

trait Revenue[F[_]]:
  def get: F[Double] 
  def add(amount: Double): F[Unit] 
  def subtract(amount: Double): F[Unit] 
  def reset: F[Unit] 
