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

package services

import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsString
import transformers.ComposedDeltaTransform
import transformers.register.CorrespondenceNameTransform

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CorrespondenceTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with OptionValues {

  val newEstateName = JsString("New Estate Name")

  "correspondenceTransformationService" - {

    "must write a corresponding transform using the transformation service" in {

      val transformationService = mock[TransformationService]
      val service = new CorrespondenceTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendCorrespondenceNameTransformer("internalId", newEstateName)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("internalId", CorrespondenceNameTransform(newEstateName))

      }
    }

    "must return a correspondence name if retrieved from transforms" - {

      "when there is a single transform" in {

        val transformationService = mock[TransformationService]
        val service = new CorrespondenceTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Seq(CorrespondenceNameTransform(newEstateName))))))

        whenReady(service.getCorrespondenceName("internalId")) { result =>

          result mustBe Some(newEstateName)

        }
      }

      "when multiple amend correspondenceName" in {

        val correspondenceName1 = JsString("New Estate Name 1")
        val correspondenceName2 = JsString("New Estate Name 2")

        val transformationService = mock[TransformationService]
        val service = new CorrespondenceTransformationService(transformationService)

        when(transformationService.getTransformations(any[String]))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(
            Seq(
              CorrespondenceNameTransform(correspondenceName1),
              CorrespondenceNameTransform(correspondenceName2)
            )
          ))))

        whenReady(service.getCorrespondenceName("internalId")) { result =>

          result mustBe Some(correspondenceName2)

        }
      }
    }
  }

}
