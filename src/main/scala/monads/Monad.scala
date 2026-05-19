package monads

trait Applicative[F[_]]:
  def pure[X](x: X): F[X]

trait Monad[M[_]] extends Applicative[M]:
  def flatMap[A, B](ma: M[A])(f: A => M[B]): M[B]
  def map[A, B](ma: M[A])(f: A => B): M[B] =
    flatMap(ma)(a => pure(f(a)))
  def flatten[A](mma: M[M[A]]): M[A] =
    flatMap(mma)(identity)

extension [M[_], X](mx: M[X])(using monad: Monad[M])
  def flatMap[Y](f: X => M[Y]): M[Y] = monad.flatMap(mx)(f)
  def map[Y](f: X => Y): M[Y] = monad.map(mx)(f)
  def flatten[Y](using ev: X <:< M[Y]): M[Y] =
    monad.flatMap(mx)(x => ev(x))
