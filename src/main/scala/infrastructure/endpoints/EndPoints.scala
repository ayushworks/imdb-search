package infrastructure.endpoints

import cats.effect.IO
import domain.{BusinessError, IllegalArgumentError, NotFoundError, SearchService}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{HttpRoutes, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._

class EndPoints(searchService: SearchService) extends Http4sDsl[IO] {

  object NameQueryParamMatcher extends QueryParamDecoderMatcher[String]("name")

  object FirstNameQueryParamMatcher extends QueryParamDecoderMatcher[String]("firstname")

  object SecondNameQueryParamMatcher extends QueryParamDecoderMatcher[String]("secondname")


  val service = HttpRoutes.of[IO] {
    case GET -> Root / "istypecasted" :? NameQueryParamMatcher(name) =>
      searchService.getTitles(name).value.flatMap {
        case Left(businessError) => matchError(businessError)
        case Right(value) => Ok(if(searchService.smellsTypeCast(value)) "yes" else "no")
      }

    case GET -> Root / "matchingtitles" :? FirstNameQueryParamMatcher(firstName) +& SecondNameQueryParamMatcher(secondName) =>
      searchService.getCommonTitles(firstName, secondName).value.flatMap {
        case Left(businessError) => matchError(businessError)
        case Right(titles) => Ok(titles.asJson)
      }

    case GET -> Root / "associationWithKB" :? NameQueryParamMatcher(name) =>
      searchService.associationWithKB(name).value.flatMap {
        case Left(businessError) => matchError(businessError)
        case Right(value) => Ok(value.toString)
      }
  }.orNotFound

  private def matchError(businessError: BusinessError): IO[Response[IO]] =
    businessError match {
      case IllegalArgumentError(msg) => BadRequest(msg)
      case NotFoundError(msg) => NotFound(msg)
    }

}
