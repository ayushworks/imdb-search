package infrastructure.repository

import cats.data.NonEmptySet
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie.util.fragments
import domain.model.{Title}


/**
  * @author Ayush Mittal
  */
class TitleRepository(transactor: Transactor[IO]) {

  def getByIds(ids: NonEmptySet[String]): IO[List[Title]] = {
    val query = fr"SELECT TCONST, TITLETYPE, PRIMARYTITLE, ORIGINALTITLE, ISADULT, STARTYEAR, ENDYEAR, RUNTIMEMINUTES, GENRES FROM titlebasics where " ++ fragments.in(fr"TCONST", ids)
    query.query[Title].to[List].transact(transactor)
  }

  def insertTitle(title: Title): IO[Title] = {
    sql"INSERT INTO titlebasics (TCONST, TITLETYPE, PRIMARYTITLE, ORIGINALTITLE, ISADULT, STARTYEAR, ENDYEAR, RUNTIMEMINUTES, GENRES) VALUES (${title.tconst}, ${title.titleType}, ${title.primaryTitle}, ${title.originalTitle}, ${title.isAdult}, ${title.startYear}, ${title.endYear}, ${title.runTimeInMinutes}, ${title.genres} )"
      .update.run.transact(transactor).map(_ => title)
  }
}
