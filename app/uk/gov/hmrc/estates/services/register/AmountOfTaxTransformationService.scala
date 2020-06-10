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

package uk.gov.hmrc.estates.services.register

import com.google.inject.Inject
import uk.gov.hmrc.estates.models.Success
import uk.gov.hmrc.estates.models.register.{AmountOfTaxOwed, TaxAmount}
import uk.gov.hmrc.estates.services.{LocalDateService, TransformationService}
import uk.gov.hmrc.estates.transformers.ComposedDeltaTransform
import uk.gov.hmrc.estates.transformers.register.AmountOfTaxOwedTransform

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class AmountOfTaxTransformationService @Inject()(
                                                  transformationService: TransformationService,
                                                  localDateService: LocalDateService
                                                ) {

  def get(internalId: String): Future[Option[AmountOfTaxOwed]] = {
    transformationService.getTransformedData("utr", internalId) map {
      case Some(ComposedDeltaTransform(transforms)) =>
        transforms.flatMap{
          case AmountOfTaxOwedTransform(amount) => Some(amount)
          case _ => None
        }.headOption
      case _ => None
    }
  }

  def addTransform(internalId: String, amount: AmountOfTaxOwed) : Future[Success.type] = {
    Future.successful(Success)
  }

}
