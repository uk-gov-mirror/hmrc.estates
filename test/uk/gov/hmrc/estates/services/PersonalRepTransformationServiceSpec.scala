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

package uk.gov.hmrc.estates.services

import java.time.LocalDate

import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.estates.models.{EstatePerRepIndType, IdentificationType, NameType}
import uk.gov.hmrc.estates.transformers.{AmendEstatePerRepIndTransform, ComposedDeltaTransform}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PersonalRepTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with OptionValues {

  private object LocalDateServiceStub extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  "PersonalRepTransformationService" - {

    "must write a corresponding transform using the transformation service" in {

      val personalRep = EstatePerRepIndType(
        name =  NameType("First", None, "Last"),
        dateOfBirth = LocalDate.of(2000,1,1),
        identification = IdentificationType(None, None, None),
        phoneNumber = "07987654",
        email = None
      )

      val transformationService = mock[TransformationService]
      val service = new PersonalRepTransformationService(transformationService, LocalDateServiceStub)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendEstatePerRepInTransformer("utr", "internalId", personalRep)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AmendEstatePerRepIndTransform(personalRep)
        )

      }
    }

    "must return a personal rep ind if retrieved from transforms" in {

      val personalRep = EstatePerRepIndType(
        name =  NameType("First", None, "Last"),
        dateOfBirth = LocalDate.of(2000,1,1),
        identification = IdentificationType(None, None, None),
        phoneNumber = "07987654",
        email = None
      )

      val transformationService = mock[TransformationService]
      val service = new PersonalRepTransformationService(transformationService, LocalDateServiceStub)

      when(transformationService.getTransformedData(any(), any()))
        .thenReturn(Future.successful(Some(ComposedDeltaTransform(Seq(AmendEstatePerRepIndTransform(personalRep))))))

      whenReady(service.getPersonalRepInd("utr", "internalId")) { result =>

        result.value mustBe personalRep

      }
    }

  }
}
