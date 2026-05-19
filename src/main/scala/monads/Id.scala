package monads

// Id[A] = A — простейшая монада, "вычисление прямо сейчас"
// в TF позволяет запускать программу синхронно: эффекты выражаются через
// мутацию состояния в полях интерпретатора (var, mutable.Map).
type Id[A] = A

given Monad[Id] with
  override def pure[A](a: A): Id[A] = a
  override def flatMap[A, B](ma: Id[A])(f: A => Id[B]): Id[B] = f(ma)
