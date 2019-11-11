package mock

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class CatsGatlingSimulation extends Simulation {

  MockUtils.startServer()

  val httpConf = http.baseUrl(System.getProperty("mock.cats.url"))

  val create = scenario("create")
    .exec(http("POST /cats")
      .post("/")
      .body(StringBody("""{ "name": "Billie" }"""))
      .check(status.is(200))
      .check(jsonPath("$.name").is("Billie"))
      .check(jsonPath("$.id")
        .saveAs("id")))

    .exec(
    http("GET /cats/{id}")
      .get("/${id}")
      .check(status.is(200))
      .check(jsonPath("$.id").is("${id}"))
      // intentional assertion failure
      .check(jsonPath("$.name").is("Billi")))
    .exitHereIfFailed
    .exec(
      http("PUT /cats/{id}")
        .put("/${id}")
        .body(StringBody("""{ "id":"${id}", "name": "Bob" }"""))
        .check(status.is(200))
        .check(jsonPath("$.id").is("${id}"))
        .check(jsonPath("$.name").is("Bob")))

    .exec(
    http("GET /cats/{id}")
      .get("/${id}")
      .check(status.is(200)))

  val delete = scenario("delete").exec(
    http("GET /cats")
      .get("/")
      .check(status.is(200))
      .check(jsonPath("$[*].id").findAll.optional
        .saveAs("ids")))

    .doIf(_.contains("ids")) {
      foreach("${ids}", "id") {
        exec(
          http("DELETE /cats/{id}")
            .delete("/${id}")
            .check(status.is(200))
            .check(bodyString.is("")))

          .exec(
          http("GET /cats/{id}")
            .get("/${id}")
            .check(status.is(404)))
      }
    }

  setUp(
    create.inject(
      constantConcurrentUsers(5) during (5 seconds),
      rampConcurrentUsers(5) to (10) during (5 seconds)
    ).protocols(httpConf)
//    delete.inject(
//      constantConcurrentUsers(5) during (10 seconds),
//      rampConcurrentUsers(5) to (10) during (10 seconds)
//    ).protocols(httpConf)
  )

}