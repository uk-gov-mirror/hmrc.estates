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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.gov.hmrc.estates.repositories.TransformationRepositoryImpl
import uk.gov.hmrc.estates.transformers.ComposedDeltaTransform
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class TransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers {

  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "addNewTransform" - {

    "must write a corresponding transform to the transformation repository with no existing transforms" in {

      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository)

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addNewTransform("utr", "internalId", ???)

      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(???))
        )
      }

    }

    "must write a corresponding transform to the transformation repository with existing transforms" in {

      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository)

      val existingTransforms = Seq(???)

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addNewTransform("utr", "internalId", ???)

      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(???))
        )
      }

    }
  }
}
