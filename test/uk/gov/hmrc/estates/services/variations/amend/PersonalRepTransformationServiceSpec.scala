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

package uk.gov.hmrc.estates.services.variations.amend

import java.time.LocalDate

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import uk.gov.hmrc.estates.models.variation.{EstatePerRepIndType, EstatePerRepOrgType, PersonalRepresentativeType}
import uk.gov.hmrc.estates.models.{IdentificationOrgType, IdentificationType, NameType}
import uk.gov.hmrc.estates.services.{LocalDateService, VariationsTransformationService}
import uk.gov.hmrc.estates.transformers.amend.AmendIndividualPersonalRepTransform
import uk.gov.hmrc.estates.utils.JsonRequests
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PersonalRepTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonRequests {
  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private val currentDate: LocalDate = LocalDate.of(1999, 3, 14)
  private object LocalDateServiceStub extends LocalDateService {
    override def now: LocalDate = currentDate
  }

  val newPersonalRepIndInfo = EstatePerRepIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = LocalDate.of(1965, 2, 10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationType(Some("newNino"), None, None),
    entityStart = LocalDate.parse("2012-03-14"),
    entityEnd = None
  )

  val newPersonalRepOrgInfo = EstatePerRepOrgType(
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

  "the amend personal rep transformation service" - {

    "must add a new amend personal rep transform using the variations transformation service" in {

      val transformationService = mock[VariationsTransformationService]
      val service = new PersonalRepTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendPersonalRepTransformer("utr", "internalId", PersonalRepresentativeType(Some(newPersonalRepIndInfo), None))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId",AmendIndividualPersonalRepTransform(newPersonalRepIndInfo))

      }
    }

    "must write a corresponding transform using the transformation service" in {
      val transformationService = mock[VariationsTransformationService]
      val service = new PersonalRepTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendPersonalRepTransformer("utr", "internalId", PersonalRepresentativeType(Some(newPersonalRepIndInfo), None))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AmendIndividualPersonalRepTransform(newPersonalRepIndInfo))

      }
    }
  }
}
