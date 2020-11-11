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

package transforms.variations

import java.time.LocalDate

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{AsyncFreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import connectors.DesConnector
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.getEstate.GetEstateResponse
import utils.JsonUtils
import uk.gov.hmrc.repositories.TransformIntegrationTest

import scala.concurrent.Future

class ClearTransformationsSpec extends AsyncFreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  val getEstateResponseFromDES: GetEstateResponse = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-response.json").as[GetEstateResponse]
  val noTransformsAppliedJson: JsValue = JsonUtils.getJsonValueFromFile("it/estates-integration-get-initial.json")

  "a clear transformations call" - {

    val stubbedDesConnector = mock[DesConnector]
    when(stubbedDesConnector.getEstateInfo(any())).thenReturn(Future.successful(getEstateResponseFromDES))

    val cc = stubControllerComponents()

    val application = new GuiceApplicationBuilder()
      .configure(Seq(
        "mongodb.uri" -> connectionString,
        "metrics.enabled" -> false,
        "auditing.enabled" -> false,
        "mongo-async-driver.akka.log-dead-letters" -> 0
      ): _*)
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Organisation)),
        bind[DesConnector].toInstance(stubbedDesConnector)
      )
      .build()

    "must return original data in a subsequent 'get' call" in assertMongoTest(application) { _ =>

      val utr: String = "5174384721"

      val transformRequest = FakeRequest(POST, s"/estates/close/$utr")
        .withBody(Json.toJson(LocalDate.parse("2000-01-01")))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val transformResult = route(application, transformRequest).get
      status(transformResult) mustBe OK

      val initialGetResult = route(application, FakeRequest(GET, s"/estates/$utr/transformed")).get
      status(initialGetResult) mustBe OK

      val clearTransformsRequest = FakeRequest(POST, s"/estates/$utr/clear-transformations")
        .withHeaders(CONTENT_TYPE -> "application/json")

      val clearTransformsResult = route(application, clearTransformsRequest).get
      status(clearTransformsResult) mustBe OK

      val subsequentGetResult = route(application, FakeRequest(GET, s"/estates/$utr/transformed")).get
      status(subsequentGetResult) mustBe OK

      contentAsJson(initialGetResult) mustNot be(noTransformsAppliedJson)
      contentAsJson(subsequentGetResult) mustBe noTransformsAppliedJson
    }
  }
}
