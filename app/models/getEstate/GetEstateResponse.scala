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

import play.api.Logger
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json._
import models.DesErrorResponse
import models.getEstate.GetEstateResponse.getClass
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

sealed trait GetEstateHttpReads {

  private val logger: Logger = Logger(getClass)

  implicit lazy val httpReads: HttpReads[GetEstateResponse] =
    new HttpReads[GetEstateResponse] {
      override def read(method: String, url: String, response: HttpResponse): GetEstateResponse = {
        logger.info(s"Response status received from des: ${response.status}")
        response.status match {
          case OK => readProcessedResponse(response)
          case BAD_REQUEST => readClientErrorResponse(response)
          case NOT_FOUND => ResourceNotFoundResponse
          case SERVICE_UNAVAILABLE => ServiceUnavailableResponse
          case _ => InternalServerErrorResponse
        }
      }
    }

  private def readProcessedResponse(response: HttpResponse) =
    response.json.validate[GetEstateResponse] match {
      case JsSuccess(estateFound,_) =>
        logger.info("Response successfully parsed as EstateFoundResponse")
        estateFound
      case JsError(errors) =>
        logger.info(s"Cannot parse as EstateFoundResponse due to $errors")
        NotEnoughDataResponse(response.json, JsError.toJson(errors))
    }

  private def readClientErrorResponse(response: HttpResponse) =
    response.json.asOpt[DesErrorResponse] match {
      case Some(desErrorResponse) =>
        desErrorResponse.code match {
          case "INVALID_UTR" =>
            InvalidUTRResponse
          case "INVALID_REGIME" =>
            InvalidRegimeResponse
          case _ =>
            BadRequestResponse
        }
      case None =>
        InternalServerErrorResponse
    }
}
