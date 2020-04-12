import cats.data.{EitherT, NonEmptySet}
import cats.effect.IO
import cats.kernel.Order
import domain.{BusinessError, ResultT}

/**
  * @author Ayush Mittal
  */
package object util {

  def nesFromSet[A](set: Set[A], ifEmpty: BusinessError)(implicit order: Order[A]): ResultT[NonEmptySet[A]] = {
    if (set.isEmpty) EitherT.leftT[IO, NonEmptySet[A]](ifEmpty)
    else EitherT.rightT[IO, BusinessError](NonEmptySet.of[A](set.head, set.tail.toList: _*))
  }
}
