# IMDB Search
A sample project using [http4s](http://http4s.org/), [doobie](http://tpolecat.github.io/doobie/),
and [circe](https://github.com/circe/circe).



## End points
The rest resources that this api provides are:

Method | Url                                               | Description
------ | -----------                                       | -----------
GET    | /istypecasted?name=actorname                      | Returns "yes" or "no" indicating if an actor is typecasted.
GET    | /matchingtitles?firstname=first&secondname=second | Returns list of matching titles for first and second actor, 404 when no matching titles are found.
POST   | /associationWithKB?name=actorname                 | Determines the degree of an actor/actress from Kavin Bacon. 404 if no such actor/actress is present



Here are some examples on how to use the microservice with curl, assuming it runs on the default port 8080:


Check if Al Pacino is typecasted:
```curl http://localhost:8080/istypecasted?name=Al%20Pacino```

Get matching list of titles for Al Pacino and Marlon Brando:
```curl http://localhost:8080/matchingtitles?firstname=Al%20Pacino&secondname=Marlon%20Brando```

Get Degree of association for Al Pacino with Kavin Bacon:
```curl http://localhost:8080/associationWithKB?name=Al%20Pacino```

# Data source
The source of the data that the api uses is [IMDb Datasets](https://www.imdb.com/interfaces/)

The data contains information about titles of movies, tv-serials , actors/actress etc. The data is relational in nature
with references between various datasets.

## How is data stored in the api?

The api uses in memory [H2 database](https://www.h2database.com/html/main.html) to store the data.

Data loading starts as the server boots up and continous in the background while the server is ready to serve request. 
# Data model

## Titles

```scala
case class Title(tconst: String, titleType: String, primaryTitle: String,
                 originalTitle: String, isAdult: String, startYear: Int,
                 endYear: Option[Int], runTimeInMinutes: Int, genres: String)
```

## Names

```scala
case class Name(nconst: String, primaryName: String, birthYear: Int, deathYear: Option[Int], primaryProfession: String, knownForTitles: String)
```

# Error handling

The api captures errors at various level : service, repository. All known business errors are represented
by a type `BusinessError`. 

All service layer methods return an `Either[BusinessError,A]` type. To be more precise , since
we use the [IO](https://typelevel.org/cats-effect/datatypes/io.html) monad to represent side effects , the type returned by most methods is
`EitherT[IO, BusinessError, A]` which is aliased as `ResultT[A]` in the entire api


## http4s
[http4s](http://http4s.org/) is used as the HTTP layer. http4s provides streaming and functional HTTP for Scala.
This example project uses [cats-effect](https://github.com/typelevel/cats-effect), but is possible to use
http4s with another effect monad.

By using an effect monad, side effects are postponed until the last moment.

http4s uses [fs2](https://github.com/functional-streams-for-scala/fs2) for streaming. This allows to return
streams in the HTTP layer so the response doesn't need to be generated in memory before sending it to the client.

In the example project this is done for the `GET /todos` endpoint.

## doobie
[doobie](http://tpolecat.github.io/doobie/) is used to connect to the database. This is a pure functional JDBC layer for Scala.
This example project uses [cats-effect](https://github.com/typelevel/cats-effect) in combination with doobie,
but doobie can use another effect monad.

Because both http4s and doobie use an effect monad, the combination is still pure and functional.

## circe
[circe](https://github.com/circe/circe) is the recommended JSON library to use with http4s. It provides
automatic derivation of JSON Encoders and Decoders.

## Configuration
[pureconfig](https://github.com/pureconfig/pureconfig) is used to read the configuration file `application.conf`.
This library allows reading a configuration into well typed objects.

## Database
[h2](http://www.h2database.com/) is used as a database. This is an in memory database, so when stopping the application, the state of the
microservice is lost.

Using [Flyway](https://flywaydb.org/) the database migrations are performed when starting the server.

## Tests
This example project contains both unit tests, which mock the infrastructure.repository that accesses the database, and
integration tests that use the [http4s](http://http4s.org/) HTTP client to perform actual requests.

## Running
You can run the microservice with `sbt run`. By default it listens to port number 8080, you can change
this in the `application.conf`.

Please copy the `title.basics.tsv` and `name.basics.tsv` in `src\main\resources` to load the required data
