package algebras

import domain.Train

// репозиторий поездов
trait TrainRepo[F[_]]:
  def all: F[List[Train]]
  def find(name: String): F[Option[Train]]
  def add(train: Train): F[Unit]
  // обновить место (true = занято, false = свободно)
  def setSeat(trainName: String, seat: String, occupied: Boolean): F[Unit]
