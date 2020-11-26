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

package models.getEstate

import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

trait GetEstateResponse

object GetEstateResponse extends GetEstateHttpReads {

  implicit val reads: Reads[GetEstateResponse] = new Reads[GetEstateResponse] {
    override def reads(json: JsValue): JsResult[GetEstateResponse] = {
      val header = (json \ "responseHeader").asOpt[ResponseHeader]

      header match {
        case Some(parsedHeader) =>
          (json \ "trustOrEstateDisplay").toOption match {
            case None =>
              JsSuccess(GetEstateStatusResponse(parsedHeader))
            case Some(x) =>
              x.validate[GetEstate] match {
                case JsSuccess(_, _) =>
                  JsSuccess(GetEstateProcessedResponse(x, parsedHeader))
                case x : JsError =>
                  JsSuccess(NotEnoughDataResponse(json, JsError.toJson(x)))
              }
          }
        case None =>
          JsSuccess(NotEnoughDataResponse(json, JsError.toJson(JsError("responseHeader not defined on response"))))
      }
    }
  }

  implicit val writes: Writes[GetEstateResponse] = Writes {
    case GetEstateProcessedResponse(estate, header) => Json.obj("responseHeader" -> header, "getEstate" -> Json.toJson(estate.as[GetEstate]))
    case GetEstateStatusResponse(header) => Json.obj("responseHeader" -> header)
    case NotEnoughDataResponse(_, errors) => Json.obj("error" -> errors)
    case _ => Json.obj("error" -> "There was an internal server error parsing response as GetEstateResponse")
  }

}

sealed trait GetEstateHttpReads extends Logging {

  implicit def httpReads(utr: String): HttpReads[GetEstateResponse] = (_: String, _: String, response: HttpResponse) => {
    response.status match {
      case OK =>
        parseOkResponse(response, utr)
      case BAD_REQUEST =>
        logger.warn(s"[UTR: $utr]" +
          s" bad request returned from des: ${response.json}")
        BadRequestResponse
      case NOT_FOUND =>
        logger.info(s"[UTR: $utr]" +
          s" trust not found in ETMP for given identifier")
        ResourceNotFoundResponse
      case SERVICE_UNAVAILABLE =>
        logger.warn(s"[UTR: $utr]" +
          s" service is unavailable, unable to get trust")
        ServiceUnavailableResponse
      case status =>
        logger.error(s"[UTR: $utr]" +
          s" error occurred when getting trust, status: $status")
        InternalServerErrorResponse
    }
  }

  private def parseOkResponse(response: HttpResponse, utr: String) : GetEstateResponse = {
    response.json.validate[GetEstateResponse] match {
      case JsSuccess(estateFound, _) => estateFound
      case JsError(errors) =>
        logger.error(s"[UTR: $utr] Cannot parse as EstateFoundResponse due to ${JsError.toJson(errors)}")
        NotEnoughDataResponse(response.json, JsError.toJson(errors))
    }
  }
}
