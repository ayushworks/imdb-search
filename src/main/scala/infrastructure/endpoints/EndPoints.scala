package infrastructure.endpoints

import cats.effect.IO
import domain.SearchService
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class EndPoints(searchService: SearchService) extends Http4sDsl[IO] {

  object NameQueryParamMatcher extends QueryParamDecoderMatcher[String]("name")

  object FirstNameQueryParamMatcher extends QueryParamDecoderMatcher[String]("firstname")

  object SecondNameQueryParamMatcher extends QueryParamDecoderMatcher[String]("secondname")


  val service = HttpService[IO] {
    case GET -> Root / "istypecasted" :? NameQueryParamMatcher(name) =>
      searchService.getTitles(name).value.flatMap {
        case Left(businessError) => NotFound(businessError.message)
        case Right(value) => Ok(if(searchService.smellsTypeCast(value)) "yes" else "no")
      }

    case GET -> Root / "matchingtitles" :? FirstNameQueryParamMatcher(firstName) +& SecondNameQueryParamMatcher(secondName) =>
      searchService.getCommonTitles(firstName, secondName).value.flatMap {
        case Left(businessError) => NotFound(businessError.message)
        case Right(titles) => Ok(titles.asJson)
      }

    case GET -> Root / "associationWithKB" :? NameQueryParamMatcher(name) =>
      searchService.associationWithKB(name).value.flatMap {
        case Left(businessError) => NotFound(businessError.message)
        case Right(value) => Ok(value.toString)
      }
  }
}
