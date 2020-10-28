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

package models.variation

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{Format, Json, OFormat}
import models.ErrorResponse
import utils.ErrorResponses._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

trait VariationResponse

final case class VariationSuccessResponse(tvn: String) extends VariationResponse

object VariationSuccessResponse {
  implicit val formats: Format[VariationSuccessResponse] = Json.format[VariationSuccessResponse]
}

case class VariationFailureResponse(response: ErrorResponse) extends VariationResponse

object VariationFailureResponse {
  implicit val formats: OFormat[VariationFailureResponse] = Json.format[VariationFailureResponse]
}

object VariationResponse {

  private val logger: Logger = Logger(getClass)

  implicit lazy val httpReads: HttpReads[VariationResponse] =
    new HttpReads[VariationResponse] {
      override def read(method: String, url: String, response: HttpResponse): VariationResponse = {

        logger.debug(s"Response body ${response.body}")

        logger.info(s"Response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.as[VariationSuccessResponse](VariationSuccessResponse.formats)
          case BAD_REQUEST if response.body contains "INVALID_CORRELATIONID" =>
            failure(InvalidCorrelationIdErrorResponse)
          case CONFLICT if response.body contains "DUPLICATE_SUBMISSION" =>
            failure(DuplicateSubmissionErrorResponse)
          case BAD_REQUEST =>
            failure(InvalidRequestErrorResponse)
          case INTERNAL_SERVER_ERROR =>
            failure(InternalServerErrorErrorResponse)
          case SERVICE_UNAVAILABLE =>
            failure(ServiceUnavailableErrorResponse)
          case status =>
            failure(ErrorResponse(status.toString, s"Error response from DES: $status"))
        }
      }
    }

  private def failure(errorResponse: ErrorResponse) = {
    logger.error(s"[failure] failed due to error: ${errorResponse.message}")
    VariationFailureResponse(errorResponse)
  }
}
