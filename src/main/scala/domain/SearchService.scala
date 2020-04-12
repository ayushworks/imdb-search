package domain

import cats.data.{EitherT, NonEmptySet}
import cats.effect.IO
import cats.instances.string._
import domain.model.Title
import infrastructure.repository.{NameRepository, TitleRepository}
import util.nesFromSet

/**
  * @author Ayush Mittal
  */
class SearchService(nameRepo: NameRepository, titleRepo: TitleRepository) {

  private val kevinBacon : String = "Kevin Bacon"

  def associationWithKB(artistName: String): ResultT[Int] = {
    if(artistName == kevinBacon) EitherT.rightT[IO, BusinessError](1)
    else {
      for {
        name <- nameRepo.getByName(artistName)
        titles = name.titleIds
        result <- EitherT.liftF(getCount(titles, 0, Set.empty[String]))
      } yield result
    }
  }

  def getCount(titleIds: Set[String], count: Int, alreadyChecked: Set[String]): IO[Int] = {
    for {
      names <- nameRepo.getByTitleIds(titleIds -- alreadyChecked)
      currentCount <- if(names.map(_.primaryName).toSet.contains(kevinBacon) || count >= 6) {
        IO.pure(count+1)
      } else {
        getCount(names.map(_.titleIds).flatten.toSet, count+1, titleIds)
      }
    } yield currentCount
  }

  def getCommonTitles(firstName: String, secondName: String): ResultT[List[Title]] = {
    for {
      names <- EitherT.liftF(nameRepo.getByNames(NonEmptySet.of(firstName, secondName)))
      commonTitles = names.foldLeft(names.headOption.map(_.titleIds).getOrElse(Set.empty[String])){
        case (common, name) => common & name.titleIds
      }
      titleIds <- nesFromSet(commonTitles, NotFoundError(s"No matching title found for $firstName and $secondName"))
      titles <- EitherT.liftF(titleRepo.getByIds(titleIds))
    } yield titles
  }

  def getTitles(artistName: String): ResultT[List[Title]] = {
    for {
      nameFromDb <- nameRepo.getByName(artistName)
      titleIds =  nameFromDb.titleIds
      titleNes <- nesFromSet(titleIds, NotFoundError(s"No matching titles for $artistName"))
      titles <- EitherT.liftF(titleRepo.getByIds(titleNes))
    } yield titles
  }

  def smellsTypeCast(titles: List[Title]): Boolean = {
    val totalWork = titles.size
    val genreCount: Map[String, Int] = titles.map(_.genreList).flatten.foldLeft(Map.empty[String, Int]){
      case (map, genre) =>
        map + (genre -> (map.get(genre).getOrElse(0) + 1))
    }
    genreCount.find(_._2 > (totalWork/2)).isDefined
  }
}
