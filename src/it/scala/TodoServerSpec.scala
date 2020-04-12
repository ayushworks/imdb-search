import cats.effect.IO
import config.{Config, Database}
import domain.SearchService
import infrastructure.endpoints.EndPoints
import io.circe.Json
import io.circe.literal._
import org.http4s.client.blaze.Http1Client
import org.http4s.{Method, Request, Status, Uri}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.http4s.server.{Server => Http4sServer}
import org.http4s.server.blaze.BlazeBuilder
import infrastructure.repository.{NameRepository, TitleRepository}

class TodoServerSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  private lazy val client = Http1Client[IO]().unsafeRunSync()

  private lazy val config = Config.load("test.conf").unsafeRunSync()

  private lazy val urlStart = s"http://${config.server.host}:${config.server.port}"

  private val server = createServer().unsafeRunSync()

  override def afterAll(): Unit = {
    client.shutdown.unsafeRunSync()
    server.shutdown.unsafeRunSync()
  }

  "Backend server" should {

    "check if Al Pacino is typecasted" in {
     val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/istypecasted?name=Al%20Pacino"))
      val stringResponse = client.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "yes"
    }

    "check if Kavin Bacon is typecasted" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/istypecasted?name=Kevin%20Bacon"))
      val stringResponse = client.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "no"
    }

    "return 404 if actor is not found" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/istypecasted?name=Whoishe"))
      val nonExistent = client.fetch(request){
        response =>
          IO.pure(response)
      }.unsafeRunSync()
      nonExistent.status shouldBe Status.NotFound
    }

    "get matching titles" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/matchingtitles?firstname=Al%20Pacino&secondname=Marlon%20Brando"))
      val json = client.expect[Json](request).unsafeRunSync()
      json shouldBe json"""
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
        """
    }

    "get Kevin Bacon number for name Five" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/associationWithKB?name=Five"))
      val stringResponse = client.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "5"
    }

    "get Kevin Bacon number for name Four" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/associationWithKB?name=Four"))
      val stringResponse = client.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "4"
    }

    "get Kevin Bacon number for name Three" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/associationWithKB?name=Three"))
      val stringResponse = client.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "3"
    }

    "get Kevin Bacon number for name Two" in {
      val request = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/associationWithKB?name=Two"))
      val stringResponse = client.expect[String](request).unsafeRunSync()
      stringResponse shouldBe "2"
    }
  }

  private def createServer(): IO[Http4sServer[IO]] = {
    for {
      transactor <- Database.transactor(config.database)
      _ <- Database.initialize(transactor)
      nameRepo = new NameRepository(transactor)
      titleRepo = new TitleRepository(transactor)
      searchService = new SearchService(nameRepo, titleRepo)
      server <- BlazeBuilder[IO]
        .bindHttp(config.server.port, config.server.host)
        .mountService(new EndPoints(searchService).service, "/").start
    } yield server
  }
}
