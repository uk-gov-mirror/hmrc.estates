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

package uk.gov.hmrc.estates.services.variations.add

import java.time.LocalDate

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import uk.gov.hmrc.estates.models.IdentificationOrgType
import uk.gov.hmrc.estates.models.variation.EstatePerRepOrgType
import uk.gov.hmrc.estates.services.VariationsTransformationService
import uk.gov.hmrc.estates.transformers.variations.add.AddBusinessPersonalRepTransform
import uk.gov.hmrc.estates.utils.JsonRequests
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PersonalRepTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonRequests {

  val newPersonalRepOrgInfo: EstatePerRepOrgType = EstatePerRepOrgType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    orgName = "Company Name",
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationOrgType(Some("UTR"), None),
    entityStart = LocalDate.parse("2012-03-14"),
    entityEnd = None
  )

  private implicit val hc : HeaderCarrier = HeaderCarrier()

  "the add personal rep transformation service" - {

    "when adding a new business personal rep" - {

      "must add a new add personal rep transform using the variations transformation service" in {

        val transformationService = mock[VariationsTransformationService]
        val service = new PersonalRepTransformationService(transformationService)

        when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

        val result = service.addAddBusinessPersonalRepTransformer("utr", "internalId", newPersonalRepOrgInfo)
        whenReady(result) { _ =>

          verify(transformationService).addNewTransform(
            "utr",
            "internalId",
            AddBusinessPersonalRepTransform(newPersonalRepOrgInfo)
          )

        }
      }
    }
  }
}
