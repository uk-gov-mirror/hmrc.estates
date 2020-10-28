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

package services.register

import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import models.register.AmountOfTaxOwed
import models.register.TaxAmount.{AmountMoreThanFiveHundredThousand, AmountMoreThanTenThousand}
import services.TransformationService
import transformers.ComposedDeltaTransform
import transformers.register.AmountOfTaxOwedTransform

import scala.concurrent.Future

class AmountOfTaxOwedTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with OptionValues {

  "AmountOfTaxTransformationService" - {

    "must write a corresponding transform using the transformation service" in {

      val transformationService = mock[TransformationService]
      val service = new AmountOfTaxTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any())).thenReturn(Future.successful(true))

      val result = service.addTransform("internalId", AmountOfTaxOwed(AmountMoreThanTenThousand))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform(
          "internalId", AmountOfTaxOwedTransform(AmountMoreThanTenThousand)
        )

      }
    }

    "must not return a transform if one does not exist" - {

      "due to there being no data" in {
        val transformationService = mock[TransformationService]
        val service = new AmountOfTaxTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))

        whenReady(service.get("internalId")) { result =>

          result mustBe None

        }
      }

      "due to there being no transforms" in {
        val transformationService = mock[TransformationService]
        val service = new AmountOfTaxTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(None))

        whenReady(service.get("internalId")) { result =>
          result mustBe None
        }
      }
    }

    "must return an amount if retrieved from transforms" - {

      "when there is a single transform" in {

        val transformationService = mock[TransformationService]
        val service = new AmountOfTaxTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(
            Seq(
              AmountOfTaxOwedTransform(AmountMoreThanFiveHundredThousand),
              AmountOfTaxOwedTransform(AmountMoreThanTenThousand))
          ))))

        whenReady(service.get("internalId")) { result =>
          result.value mustBe AmountOfTaxOwed(AmountMoreThanTenThousand)
        }
      }
    }
  }
}
