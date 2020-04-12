import cats.effect.IO
import config.{Config, Database}
import domain.SearchService
import fs2.{Stream, StreamApp}
import fs2.StreamApp.ExitCode
import infrastructure.endpoints.EndPoints
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import infrastructure.repository.{NameRepository, TitleRepository}

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] with Http4sDsl[IO] {
  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    for {
      config <- Stream.eval(Config.load())
      transactor <- Stream.eval(Database.transactor(config.database))
      _ <- Stream.eval(Database.initialize(transactor))
      nameRepo = new NameRepository(transactor)
      titleRepo = new TitleRepository(transactor)
      searchService = new SearchService(nameRepo, titleRepo)
      httpService = new EndPoints(searchService)
      exitCode <- BlazeBuilder[IO]
        .bindHttp(config.server.port, config.server.host)
        .mountService(httpService.service, "/")
        .serve
    } yield exitCode
  }
}
