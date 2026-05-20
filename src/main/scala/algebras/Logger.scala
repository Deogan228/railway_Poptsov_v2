package algebras

// логгер: накапливает строки, take возвращает накопленное и очищает буфер, тоже абстракт
trait Logger[F[_]]:
  def add(line: String): F[Unit] //добавляет строку в лог
  def addAll(lines: List[String]): F[Unit] //добавляет несколько строк в лог
  def take: F[List[String]] //возвращает накопленные строки и очищает буфер
