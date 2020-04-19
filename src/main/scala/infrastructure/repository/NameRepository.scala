package infrastructure.repository

import cats.data.EitherT
import cats.effect.IO
import doobie.util.transactor.Transactor
import domain.model.{Name}
import doobie.implicits._
import cats.data.NonEmptySet
import domain.{NotFoundError, ResultT}
import doobie.util.fragment.Fragment
import doobie.util.fragments

/**
  * @author Ayush Mittal
  */
class NameRepository(transactor: Transactor[IO]) {

  def getByName(artistName: String): ResultT[Name] = {
    val queryFragment = fr"SELECT NCONST, PRIMARYNAME, BIRTHYEAR, DEATHYEAR, PRIMARYPROFESSION, KNOWNFORTITLES FROM names where" ++ fr"LOWER(PRIMARYNAME) = ${artistName.toLowerCase}"
    val query = queryFragment.query[Name]
    EitherT(query.option.transact(transactor).map{
      case Some(name) => Right(name)
      case None => Left(NotFoundError(s"artist with primary name $artistName not found"))
    })
  }

  def getByTitleIds(titleIds: Set[String]): IO[List[Name]] = {
    val orConditions = titleIds.map(titleId => Fragment.const("KNOWNFORTITLES") ++ fr"LIKE ${"%" +titleId +"%"}").toSeq
    val query = fr"SELECT NCONST, PRIMARYNAME, BIRTHYEAR, DEATHYEAR, PRIMARYPROFESSION, KNOWNFORTITLES FROM names" ++ fragments.whereOr(orConditions: _*)
    query.query[Name].to[List].transact(transactor)
  }

  def getByNames(names: NonEmptySet[String]): IO[List[Name]] = {
    val query = fr"SELECT NCONST, PRIMARYNAME, BIRTHYEAR, DEATHYEAR, PRIMARYPROFESSION, KNOWNFORTITLES FROM names where " ++ fragments.in(fr"PRIMARYNAME", names)
    query.query[Name].to[List].transact(transactor)
  }

  def insertTitle(name: Name): IO[Name] = {
    sql"INSERT INTO names (NCONST, PRIMARYNAME, BIRTHYEAR, DEATHYEAR, PRIMARYPROFESSION, KNOWNFORTITLES) VALUES (${name.nconst}, ${name.primaryName}, ${name.birthYear}, ${name.deathYear}, ${name.primaryProfession}, ${name.knownForTitles})"
      .update.run.transact(transactor).map(_ => name)
  }
}
