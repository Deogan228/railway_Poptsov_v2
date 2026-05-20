package monads

type Id[A] = A

given Monad[Id] with
  override def pure[A](a: A): Id[A] = a 
  override def flatMap[A, B](ma: Id[A])(f: A => Id[B]): Id[B] = f(ma) 
