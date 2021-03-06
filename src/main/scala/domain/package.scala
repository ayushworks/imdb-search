import cats.data.EitherT
import cats.effect.IO

package object domain {
  sealed trait BusinessError {
    val message: String
  }

  case class IllegalArgumentError(message: String) extends BusinessError

  case class NotFoundError(message: String) extends BusinessError

  type ResultT[A] = EitherT[IO, BusinessError, A]
}
