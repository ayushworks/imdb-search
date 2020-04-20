import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO, Timer}
import config.{Config, Database}
import domain.{DataLoader, SearchService}
import infrastructure.endpoints.EndPoints
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.http4s.server.blaze.BlazeServerBuilder
import infrastructure.repository.{NameRepository, TitleRepository}
import cats.implicits._
import io.circe.Json
import org.http4s.{Method, Request, Status, Uri}
import org.http4s.client.{Client, JavaNetClientBuilder}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContext
import io.circe._
import io.circe.literal._
import org.http4s.circe._


class TodoServerSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  implicit val timer: Timer[IO] = IO.timer(global)

  private lazy val config = Config.load("test.conf").unsafeRunSync()

  private val fiber = runServer.unsafeRunSync()

  val blockingEC = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(5))

  val httpClient: Client[IO] = JavaNetClientBuilder[IO](blockingEC).create

  private lazy val urlStart = s"http://${config.server.host}:${config.server.port}"



  override def afterAll(): Unit = {
    fiber.cancel.unsafeRunSync()
  }


  "Backend server" should {

    "check if Al Pacino is typecasted" in {
     val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/istypecasted?name=Al%20Pacino"))
      val stringResponse = httpClient.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "yes"
    }

    "return 404 if actor is not found" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/istypecasted?name=Whoishe"))
      val nonExistent = httpClient.fetch(request){
        response =>
          IO.pure(response)
      }.unsafeRunSync()
      nonExistent.status shouldBe Status.NotFound
    }

    "get matching titles" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/matchingtitles?firstname=Al%20Pacino&secondname=Marlon%20Brando"))
      val response = httpClient.expect[Json](request).unsafeRunSync()
      /*response shouldBe json"""
       [
         {
           "tconst": "tt0068646",
           "titleType": "movie",
           "primaryTitle": "The Godfather",
           "originalTitle": "The Godfather",
           "isAdult": "0",
           "startYear": 1972,
           "endYear": null,
           "runTimeInMinutes": 175,
           "genres": "Crime,Drama"
         }
       ]
        """*/
    }

    "get Kevin Bacon number for name Five" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/associationWithKB?name=Five"))
      val stringResponse = httpClient.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "5"
    }

    "get Kevin Bacon number for name Four" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/associationWithKB?name=Four"))
      val stringResponse = httpClient.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "4"
    }

    "get Kevin Bacon number for name Three" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/associationWithKB?name=Three"))
      val stringResponse = httpClient.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "3"
    }

    "get Kevin Bacon number for name Two" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/associationWithKB?name=Two"))
      val stringResponse = httpClient.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "2"
    }
  }




  def runServer = {
    for {
      config <- Config.load("test.conf")
      transactor <- Database.transactor(config.database)
      _ <- Database.initialize(transactor)
      nameRepo = new NameRepository(transactor)
      titleRepo = new TitleRepository(transactor)
      dbLoader = new DataLoader
      _ <- IO.shift *> dbLoader.load(file = config.files.names , rowExtractor = DataLoader.dataToName, rowSaver = nameRepo.insertName)
      _ <- IO.shift *> dbLoader.load(file = config.files.titles , rowExtractor = DataLoader.dataToTitle, rowSaver = titleRepo.insertTitle)
      searchService = new SearchService(nameRepo, titleRepo)
      httpService = new EndPoints(searchService)
      exitCode <- run(httpService, config).use(_ => IO.never).start
    } yield exitCode
  }

  def run(endPoints: EndPoints, config: Config) =
    BlazeServerBuilder[IO].bindHttp(config.server.port, config.server.host).withHttpApp(endPoints.service).resource
}
