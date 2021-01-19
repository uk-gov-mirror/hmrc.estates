/*
 * Copyright 2021 HM Revenue & Customs
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

package models.getEstate

import play.api.libs.json.{JsValue, Json}
import utils.ErrorResponses.EtmpDataStaleErrorResponse

sealed trait GetEstateErrorResponse extends GetEstateResponse {
  override def toString: String = super.toString
}

case object InvalidUTRResponse extends GetEstateErrorResponse {
  override def toString: String = "The UTR provided is invalid"
}

case object BadRequestResponse extends GetEstateErrorResponse {
  override def toString: String = "Bad Request received from DES"
}

case object ResourceNotFoundResponse extends GetEstateErrorResponse {
  override def toString: String = "Not Found received from DES"
}

case object InternalServerErrorResponse extends GetEstateErrorResponse {
  override def toString: String = "Internal Server Error received from DES"
}

case class NotEnoughDataResponse(json: JsValue, errors: JsValue) extends GetEstateErrorResponse {
  override def toString: String = Json.stringify(errors)
}

case object ServiceUnavailableResponse extends GetEstateErrorResponse {
  override def toString: String = "Service Unavailable received from DES"
}

case class TransformationErrorResponse(errors: String) extends GetEstateErrorResponse {
  override def toString: String = errors
}

case object EtmpCacheDataStaleResponse extends GetEstateErrorResponse {
  override def toString: String = EtmpDataStaleErrorResponse.message
}
