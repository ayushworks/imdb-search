package domain

import cats.{Parallel, Traverse}
import cats.effect.concurrent.Semaphore
import cats.effect.{Concurrent, ContextShift, IO, Resource, Timer}
import com.github.tototoshi.csv.{CSVReader, TSVFormat}
import domain.model.{Name, Title}
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream

import scala.io.Source
import scala.util.Try
import scala.concurrent.duration._

/**
  * @author Ayush Mittal
  */
class DataLoader extends LazyLogging {

  def load[A](file : String, rowExtractor: Map[String,String] => A, rowSaver: A => IO[A])(implicit F: Concurrent[IO], P: Parallel[IO], CS: ContextShift[IO], T: Timer[IO]) = {
    loadFile(file).use {
      reader =>
        val fs2Stream = Stream.fromIterator(reader.iteratorWithHeaders).evalMap(x => rowSaver(rowExtractor(x)))
        fs2Stream.chunkLimit(100).evalTap(chunk => IO(println(s"Processing batch of ${chunk.size} elements"))).compile.toList
    }
  }

  /*private def parTraverseN[A,B](n: Int, ga: Stream[A])(f: A => IO[B])(implicit F: Concurrent[IO], P: Parallel[IO], CS: ContextShift[IO])=
    Semaphore[IO](n).flatMap {
      s =>
        ga.parTraverse(a => s.withPermit(CS.shift *> f(a).onError{
          case t: Throwable =>
            logger.error(s"unable to insert data in database", t)
            IO.unit
        }.flatTap{
          row => IO(logger.info(s"extracting and inserted $row"))
        }))
    }*/

  private def loadFile(file: String): Resource[IO, CSVReader] = {
    Resource.make{
      IO(CSVReader.open(Source.fromResource(file))(tsv)).onError{
        case t: Throwable =>
          logger.error(s"unable to load $file", t)
          IO.unit
      }
    }{
      reader =>
        IO(reader.close).handleErrorWith{
          t =>
            logger.error(s"reader close failed", t)
            IO.unit
        }
    }
  }


}

object DataLoader {
  def dataToName(map: Map[String, String]): Name = {
    Try{
      val nconst = map.getOrElse("nconst","")
      val primaryName = map.getOrElse("primaryName", "")
      val birthYear = {
        val value = map.getOrElse("birthYear","0000")
        if(value.equals(tsv.nullChar)) 0 else value.toInt
      }
      val deathYear = {
        val value = map.getOrElse("deathYear","0000")
        if(value.equals(tsv.nullChar)) None else value.toInt.some
      }
      val primaryProfession = map.getOrElse("primaryProfession","")
      val knownForTitles = map.getOrElse("knownForTitles","")
      Name(nconst, primaryName, birthYear, deathYear, primaryProfession, knownForTitles)
    }.recover {
      case _: Throwable =>
        Name("", "", 0, None, "", "")
    }.get
  }

  def dataToTitle(map: Map[String, String]): Title = {
    Try{
      val tconst = map.getOrElse("tconst","")
      val titleType = map.getOrElse("titleType", "")
      val primaryTitle = map.getOrElse("primaryTitle", "")
      val originalTitle = map.getOrElse("originalTitle", "")
      val isAdult = map.getOrElse("isAdult", "")
      val startYear = {
        val value = map.getOrElse("startYear","0000")
        if(value.equals(tsv.nullChar)) 0 else value.toInt
      }
      val endYear = {
        val value = map.getOrElse("endYear","0000")
        if(value.equals(tsv.nullChar)) None else value.toInt.some
      }
      val runTimeInMinutes = {
        val value = map.getOrElse("runTimeInMinutes","0")
        if(value.equals(tsv.nullChar)) 0 else value.toInt
      }
      val genres = map.getOrElse("genres", "")
      Title(tconst, titleType, primaryTitle, originalTitle, isAdult, startYear, endYear, runTimeInMinutes, genres)
    }.recover {
      case _: Throwable =>
        Title("", "", "", "", "", 0, None, 0, "")
    }.get
  }
}

object tsv extends TSVFormat {
  //default escape char leads to error in reading null values
  override val escapeChar: Char = '~'
  val nullChar = "\\N"
}
