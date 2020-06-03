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

import play.api.test.Helpers.running

import org.scalatest.{FreeSpec, MustMatchers}
import uk.gov.hmrc.estates.repositories.TransformationRepository
import uk.gov.hmrc.estates.transformers.ComposedDeltaTransform
import scala.concurrent.ExecutionContext.Implicits.global

class TransformRepositorySpec extends FreeSpec with MustMatchers with TransformIntegrationTest {

  "a transform repository" - {

    "must be able to store and retrieve a payload" in {

      val application = applicationBuilder.build()

      running(application) {
        getConnection(application).map { connection =>

          dropTheDatabase(connection)

          val repository = application.injector.instanceOf[TransformationRepository]

          val storedOk = repository.set("UTRUTRUTR", "InternalId", data)
          storedOk.futureValue mustBe true

          val retrieved = repository.get("UTRUTRUTR", "InternalId")
            .map(_.getOrElse(fail("The record was not found in the database")))

          retrieved.futureValue mustBe data

          dropTheDatabase(connection)
        }.get
      }
    }
  }

  val data = ComposedDeltaTransform(
    Seq()
  )
}