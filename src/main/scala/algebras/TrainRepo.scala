package algebras

import domain.Train

// репозиторий поездов
trait TrainRepo[F[_]]:
  def all: F[List[Train]] //список всех поездов
  def find(name: String): F[Option[Train]] //поиск поезда по имени
  def add(train: Train): F[Unit] //добавить поезд
  
  // обновить место (true = занято, false = свободно)
  def setSeat(trainName: String, seat: String, occupied: Boolean): F[Unit] //пометить место как занятое или свободное
