package tf

import monads.{*, given}
import algebras.Console

// дерево меню, параметризованное F[_].
// каждая опция — либо лист с действием, либо вложенное поддерево.
sealed trait MenuOption[F[_]]:
  def title: String

case class MenuLeaf[F[_]](title: String, action: () => F[Unit]) extends MenuOption[F]
case class MenuTreeNode[F[_]](title: String, options: Seq[MenuOption[F]]) extends MenuOption[F]

object Menu:
  val ExitCommand = 0

  // рекурсивный цикл: показать -> прочитать -> обработать -> повторить.
  // headerSuffix — динамическая часть заголовка (выручка, статус кассы).
  def loop[F[_]: Monad](
      node: MenuTreeNode[F],
      headerSuffix: () => F[String]
  )(using console: Console[F]): F[Unit] =
    val F = summon[Monad[F]]

    def show: F[Unit] =
      for
        suf <- headerSuffix()
        items = node.options.zipWithIndex
          .map { case (o, i) => s"  ${i + 1}  ${o.title}" }
          .mkString("\n")
        _ <- console.write(s"\n${node.title}$suf\n$items\n  $ExitCommand  выход\n  выбор: ")
      yield ()

    def execAt(idx: Int): F[Unit] =
      node.options(idx) match
        case MenuLeaf(_, action) => action()
        // typed-pattern на параметризованный тип даёт unchecked-warning из-за erasure,
        // но в нашем случае MenuTreeNode[F] получается из node.options того же F — безопасно
        case sub: MenuTreeNode[?] => loop(sub.asInstanceOf[MenuTreeNode[F]], headerSuffix)

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

    def iter: F[Unit] = step.flatMap(c => if c then iter else F.pure(()))
    iter
