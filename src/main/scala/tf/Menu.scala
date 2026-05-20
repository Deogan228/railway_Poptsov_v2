package tf

import monads.{*, given}
import algebras.Console

// дерево меню, параметризованное F[_].
sealed trait MenuOption[F[_]]:
  def title: String

case class MenuLeaf[F[_]](title: String, action: () => F[Unit]) extends MenuOption[F] //либо лист с действием
case class MenuTreeNode[F[_]](title: String, options: Seq[MenuOption[F]]) extends MenuOption[F] //либо вложенное поддерево

object Menu:
  val ExitCommand = 0 //константа для выхода

  // рекурсивный цикл: показать -> прочитать -> обработать -> повторить.
  def loop[F[_]: Monad](
      node: MenuTreeNode[F],
      headerSuffix: () => F[String] //функция которая возвращает динамическую строку для заголовка, например, с текущей выручкой
  )(using console: Console[F]): F[Unit] =
    val F = summon[Monad[F]]

    //получаем суффикс, формируем и показываем меню
    def show: F[Unit] =
      for
        suf <- headerSuffix()
        items = node.options.zipWithIndex
          .map { case (o, i) => s"  ${i + 1}  ${o.title}" }
          .mkString("\n")
        _ <- console.write(s"\n${node.title}$suf\n$items\n  $ExitCommand  выход\n  выбор: ")
      yield ()

    //выполняем пункт по индексу
    def execAt(idx: Int): F[Unit] =
      node.options(idx) match
        case MenuLeaf(_, action) => action() //вызываем действие
        case sub: MenuTreeNode[?] => loop(sub.asInstanceOf[MenuTreeNode[F]], headerSuffix) //запускаем loop для подменю

    //шаг: показать, прочитать, выполнить, вернуть флаг продолжения
    def step: F[Boolean] =
      for
        _     <- show
        input <- console.readLine
        cont  <- input.trim.toIntOption match
          case Some(ExitCommand) => F.pure(false)
          case Some(i) if i >= 1 && i <= node.options.size =>
            execAt(i - 1).map(_ => true)
          case _ =>
            console.writeLine("  неизвестная команда").map(_ => true)
      yield cont

    //если step вернул true, повторяем, иначе выходим
    def iter: F[Unit] = step.flatMap(c => if c then iter else F.pure(()))
    iter
