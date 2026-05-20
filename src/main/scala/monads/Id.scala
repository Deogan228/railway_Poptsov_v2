package monads

//Id - псевдоним для типа, который просто возвращает значение без обертки.
// то есть это монада которая ничего не делает
type Id[A] = A

given Monad[Id] with
  override def pure[A](a: A): Id[A] = a // просто возвращаем значение
  override def flatMap[A, B](ma: Id[A])(f: A => Id[B]): Id[B] = f(ma) // просто вызывает функцию с переданным значением
