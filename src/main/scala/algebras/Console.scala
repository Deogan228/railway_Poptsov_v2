package algebras

// консольный ввод-вывод абстрагированный над F
trait Console[F[_]]:
  def writeLine(s: String): F[Unit]
  def write(s: String): F[Unit]
  def readLine: F[String]
