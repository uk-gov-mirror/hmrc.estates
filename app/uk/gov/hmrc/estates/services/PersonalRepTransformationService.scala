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

import javax.inject.Inject
import uk.gov.hmrc.estates.models.EstatePerRepIndType
import uk.gov.hmrc.estates.transformers.{AmendEstatePerRepIndTransform, ComposedDeltaTransform}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class PersonalRepTransformationService @Inject()(
                                                  transformationService: TransformationService,
                                                  localDateService: LocalDateService
                                                )(implicit val ec: ExecutionContext) {

  def addAmendEstatePerRepInTransformer(utr: String, internalId: String, newPersonalRep: EstatePerRepIndType): Future[Success.type] =
    transformationService.addNewTransform(utr, internalId, AmendEstatePerRepIndTransform(newPersonalRep)).map(_ => Success)

  def getPersonalRepInd(utr: String, internalId: String): Future[Option[EstatePerRepIndType]] = {
    transformationService.getTransformedData(utr, internalId) map {
      case Some(ComposedDeltaTransform(transforms)) =>
        transforms.flatMap{
          case AmendEstatePerRepIndTransform(personalRep) => Some(personalRep)
          case _ => None
        }.headOption
      case _ => None
    }
  }

}
