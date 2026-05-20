package algebras

trait Console[F[_]]:
  def writeLine(s: String): F[Unit]
  def write(s: String): F[Unit] 
  def readLine: F[String] 