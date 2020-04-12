package domain.model

/**
  * @author Ayush Mittal
  */
case class Title(tconst: String, titleType: String, primaryTitle: String,
                 originalTitle: String, isAdult: String, startYear: Int,
                 endYear: Option[Int], runTimeInMinutes: Int, genres: String) {

  def genreList: List[String] = genres.split(",").toList
}
