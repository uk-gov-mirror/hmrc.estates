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

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.JsObject
import repositories.TransformationRepository
import transformers.register.YearsReturnsTransform
import transformers.{ComposedDeltaTransform, DeltaTransform}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformationService @Inject()(repository: TransformationRepository){

  private val logger: Logger = Logger(getClass)
  
  def addNewTransform(internalId: String, newTransform: DeltaTransform) : Future[Boolean] = {
    repository.get(internalId) map {
      case None =>
        ComposedDeltaTransform(Seq(newTransform))

      case Some(composedTransform) =>
        composedTransform :+ newTransform

    } flatMap { newTransforms =>
      repository.set(internalId, newTransforms)
    } recoverWith {
      case e =>
        logger.error(s"[TransformationService] exception adding new transform: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def removeYearsReturnsTransform(internalId: String) : Future[Boolean] = {
    repository.get(internalId) map {
      case None =>
        ComposedDeltaTransform(Seq())

      case Some(composedTransform) =>
        ComposedDeltaTransform(composedTransform.deltaTransforms.filterNot(_.isInstanceOf[YearsReturnsTransform]))

    } flatMap { newTransforms =>
      repository.set(internalId, newTransforms)
    } recoverWith {
      case e =>
        logger.error(s"[TransformationService] exception removing transform: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def getTransformations(internalId: String): Future[Option[ComposedDeltaTransform]] =
    repository.get(internalId)

  def removeAllTransformations(internalId: String): Future[Option[JsObject]] =
    repository.resetCache(internalId)
}
