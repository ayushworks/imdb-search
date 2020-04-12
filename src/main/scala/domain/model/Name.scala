package domain.model

/**
  * @author Ayush Mittal
  */
case class Name(nconst: String, primaryName: String, birthYear: Int, deathYear: Option[Int], primaryProfession: String, knownForTitles: String) {
  def professions: List[String] = primaryProfession.split(",").toList
  def titleIds: Set[String] = knownForTitles.split(",").toSet
}
