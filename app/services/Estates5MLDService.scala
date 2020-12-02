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

import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Estates5MLDService @Inject()(estatesStoreService: EstatesStoreService){

  def is5mldEnabled()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    estatesStoreService.isFeatureEnabled("5mld")

  def applySubmissionDate(registration: JsValue, is5mld: Boolean): JsResult[JsValue] = {
    if (is5mld) {
      registration.transform(
        __.json.update((__ \ 'submissionDate).json.put(Json.toJson(LocalDate.now())))
      )
    } else {
      JsSuccess(registration)
    }
  }

}
