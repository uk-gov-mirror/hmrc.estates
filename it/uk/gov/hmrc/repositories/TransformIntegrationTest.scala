/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.repositories

import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{running, stubControllerComponents}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{DefaultDB, MongoConnection}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.estates.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.estates.repositories.EstatesMongoDriver

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

trait TransformIntegrationTest extends ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(500, Millis))

  val connectionString = "mongodb://localhost:27017/estates-integration"

  def getDatabase(connection: MongoConnection): Future[DefaultDB] = {
    connection.database("estates-integration")
  }

  def getConnection(application: Application): Try[MongoConnection] = {
    val mongoDriver = application.injector.instanceOf[ReactiveMongoApi]
    for {
      uri <- MongoConnection.parseURI(connectionString)
      connection <- mongoDriver.driver.connection(uri, strictUri = true)
    } yield connection
  }

  def dropTheDatabase(connection: MongoConnection) = {
    getDatabase(connection).flatMap(_.drop())
  }

  private val cc = stubControllerComponents()

  def application : Application = new GuiceApplicationBuilder()
    .configure(Seq(
    "mongodb.uri" -> connectionString,
    "metrics.enabled" -> false,
    "auditing.enabled" -> false,
    "mongo-async-driver.akka.log-dead-letters" -> 0
  ): _*)
    .overrides(
    bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Organisation))
  ).build()

  def assertMongoTest(application: Application)(block: Application => Assertion): Future[Assertion] =
    running(application) {
      for {
        connection <- Future.fromTry(getConnection(application))
        _ <- dropTheDatabase(connection)
      } yield block(application)
    }
}
