import cats.effect.{ExitCode, IO, IOApp}
import config.{Config, Database}
import domain.{DataLoader, SearchService}
import infrastructure.endpoints.EndPoints
import org.http4s.server.blaze.BlazeServerBuilder
import infrastructure.repository.{NameRepository, TitleRepository}
import cats.implicits._

object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    for {
      config <- Config.load()
      transactor <- Database.transactor(config.database)
      _ <- Database.initialize(transactor)
      nameRepo = new NameRepository(transactor)
      titleRepo = new TitleRepository(transactor)
      dbLoader = new DataLoader
      _ <- IO.shift *> dbLoader.load(file = config.files.names , rowExtractor = DataLoader.dataToName, rowSaver = nameRepo.insertTitle).start
      _ <- IO.shift *> dbLoader.load(file = config.files.titles , rowExtractor = DataLoader.dataToTitle, rowSaver = titleRepo.insertTitle).start
      searchService = new SearchService(nameRepo, titleRepo)
      httpService = new EndPoints(searchService)
      exitCode <- run(httpService, config)
    } yield exitCode
  }

  def run(endPoints: EndPoints, config: Config): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(config.server.port, config.server.host)
      .withHttpApp(endPoints.service)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
