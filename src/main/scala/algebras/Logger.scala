package algebras

trait Logger[F[_]]:
  def add(line: String): F[Unit] 
  def addAll(lines: List[String]): F[Unit] 
  def take: F[List[String]] 
