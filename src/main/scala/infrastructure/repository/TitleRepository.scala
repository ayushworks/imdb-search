package infrastructure.repository

import cats.data.{NonEmptySet}
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie.util.fragments
import domain.model.Title


/**
  * @author Ayush Mittal
  */
class TitleRepository(transactor: Transactor[IO]) {

  def getByIds(ids: NonEmptySet[String]): IO[List[Title]] = {
    val query = fr"SELECT TCONST, TITLETYPE, PRIMARYTITLE, ORIGINALTITLE, ISADULT, STARTYEAR, ENDYEAR, RUNTIMEMINUTES, GENRES FROM titlebasics where " ++ fragments.in(fr"TCONST", ids)
    query.query[Title].to[List].transact(transactor)
  }
}
