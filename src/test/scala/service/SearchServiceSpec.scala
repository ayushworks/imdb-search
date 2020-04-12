package service

import cats.data.EitherT
import cats.effect.IO
import domain.SearchService
import domain.model.{Name, Title}
import fs2.Stream
import infrastructure.endpoints.EndPoints
import io.circe.Json
import io.circe.literal._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.{Request, Response, Status, Uri}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{EitherValues, Matchers, WordSpec}
import infrastructure.repository.{NameRepository, TitleRepository}
import cats.data.NonEmptySet
import cats.instances.string._
import org.scalatest.concurrent.ScalaFutures

class ServiceSpec extends WordSpec with ScalaFutures with MockFactory with Matchers with EitherValues {
  private val nameRepository = stub[NameRepository]
  private val titleRepository = stub[TitleRepository]

  private val service = new SearchService(nameRepository, titleRepository)

  "Search service" should {
    "get titles for actor/actress" in {
      val artist = Name("nameid","ayush",1989,None,"actor","t1,t2")
      val title1 = Title("t1","movie","title1","title1","0",1989,None,120,"drama")
      val title2 = Title("t2","movie","title2","title2","0",1989,None,120,"drama")

      (nameRepository.getByName _).when("ayush").returns(EitherT.liftF(IO.pure(artist)))
      (titleRepository.getByIds _).when(NonEmptySet.of("t1", "t2")).returns(IO.pure(List(title1, title2)))

      val titles = service.getTitles("ayush").value.unsafeToFuture()
      whenReady(titles) {
        result =>
          result.isRight shouldBe true
          result.right.value shouldBe List(title1, title2)
      }
    }

    "smell typecasts with title list" in {
      val title1 = Title("t1","movie","title1","title1","0",1989,None,120,"comedy,romance,drama")
      val title2 = Title("t2","movie","title2","title2","0",1989,None,120,"horror,drama")
      val title3 = Title("t3","movie","title3","title3","0",1989,None,120,"documentaty,drama")
      val title4 = Title("t4","movie","title4","title4","0",1989,None,120,"musical")
      val result = service.smellsTypeCast(List(title1, title2, title3, title4))
      result shouldBe true
    }

    "search for common titles" in {
      val artist1 = Name("nameid","ayush",1989,None,"actor","t1,t2,t3")
      val artist2 = Name("nameid","ayush",1989,None,"actor","t3,t4")
      val title3 = Title("t3","movie","title3","title3","0",1989,None,120,"documentaty,drama")
      (nameRepository.getByNames _).when(NonEmptySet.of("actor1", "actor2")).returns(IO.pure(List(artist1, artist2)))
      (titleRepository.getByIds _).when(NonEmptySet.of("t3")).returns(IO.pure(List(title3)))
      val titles = service.getCommonTitles("actor1", "actor2").value.unsafeToFuture()
      whenReady(titles){
        result =>
          result.isRight shouldBe true
          result.right.value shouldBe List(title3)
      }
    }

    "search for common titles" in {
      val artist1 = Name("nameid","ayush",1989,None,"actor","t1,t2,t3")
      val artist2 = Name("nameid","ayush",1989,None,"actor","t3,t4")
      val title3 = Title("t3","movie","title3","title3","0",1989,None,120,"documentaty,drama")
      (nameRepository.getByNames _).when(NonEmptySet.of("actor1", "actor2")).returns(IO.pure(List(artist1, artist2)))
      (titleRepository.getByIds _).when(NonEmptySet.of("t3")).returns(IO.pure(List(title3)))
      val titles = service.getCommonTitles("actor1", "actor2").value.unsafeToFuture()
      whenReady(titles){
        result =>
          result.isRight shouldBe true
          result.right.value shouldBe List(title3)
      }
    }
  }
}
