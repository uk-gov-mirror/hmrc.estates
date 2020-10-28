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

package controllers

import base.BaseSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector


class TaxEnrolmentCallbackControllerSpec extends BaseSpec with GuiceOneServerPerSuite {

  val mockAuditConnector = mock[AuditConnector]

  ".subscriptionCallback" should {

    "return 200 " when {

      "tax enrolment callback for subscription id enrolment" in {

        val application = applicationBuilder()
          .overrides(
            bind[AuditConnector].toInstance(mockAuditConnector)
          ).build()

        val controller = application.injector.instanceOf[TaxEnrolmentCallbackController]

        val result = controller.subscriptionCallback().apply(postRequestWithPayload(Json.parse({"""{ "url" : "http//","state" : "SUCCESS"}"""})))
        status(result) mustBe OK
      }
    }
  }

}
