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

import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers.running
import org.scalatest.{Assertion, AsyncFreeSpec, EitherValues, FreeSpec, Inside, MustMatchers, OptionValues}
import play.api.Application
import play.api.libs.json.Json
import uk.gov.hmrc.estates.repositories.TransformationRepository
import uk.gov.hmrc.estates.transformers.ComposedDeltaTransform

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformRepositorySpec  extends AsyncFreeSpec with MustMatchers
  with ScalaFutures with OptionValues with Inside with TransformIntegrationTest with EitherValues {

  "a transform repository" - {

    val internalId = "Int-328969d0-557e-4559-96ba-074d0597107e"

    def assertMongoTest(application: Application)(block: Application => Assertion): Future[Assertion] =
      running(application) {
        for {
          connection <- Future.fromTry(getConnection(application))
          _ <- dropTheDatabase(connection)
        } yield block(application)
      }

    "must be able to store and retrieve a payload" in assertMongoTest(application) { app =>

      val repository = app.injector.instanceOf[TransformationRepository]

      val storedOk = repository.set(internalId, data)
      storedOk.futureValue mustBe true

      val retrieved = repository.get(internalId)
        .map(_.getOrElse(fail("The record was not found in the database")))

      retrieved.futureValue mustBe data
    }
  }

  private val data = ComposedDeltaTransform(
    Seq()
  )
}
