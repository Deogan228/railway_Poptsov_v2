package algebras

// консольный ввод-вывод абстрагированный над F
trait Console[F[_]]:
  def writeLine(s: String): F[Unit] //вывод с переносом строки
  def write(s: String): F[Unit] //вывод без переноса строки
  def readLine: F[String] //чтение строки с клавиатуры
