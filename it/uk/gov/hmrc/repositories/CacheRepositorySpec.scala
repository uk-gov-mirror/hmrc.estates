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
import org.scalatest._
import play.api.libs.json.Json
import uk.gov.hmrc.estates.repositories.CacheRepository

import scala.concurrent.ExecutionContext.Implicits.global

class CacheRepositorySpec extends AsyncFreeSpec with MustMatchers
  with ScalaFutures with OptionValues with Inside with TransformIntegrationTest with EitherValues {

  "a cache repository" - {

    val internalId = "Int-328969d0-96ba-4559-557e-074d0597107e"

    "must be able to store and retrieve a payload" in assertMongoTest(createApplication) { app =>

      println("--- 1 ---")

      val repository = app.injector.instanceOf[CacheRepository]
      println("--- 2 ---")

      val storedOk = repository.set("UTRUTRUTR", internalId, data)
      println("--- 3 ---")
      storedOk.futureValue mustBe true

      println("--- 4 ---")
      val retrieved = repository.get("UTRUTRUTR", internalId)
      println("--- 5 ---")

      retrieved.futureValue mustBe Some(data)
      println("--- 6 ---")

      "A" mustBe "A"

    }
  }

  private val data = Json.obj("testField" -> "testValue")

}
