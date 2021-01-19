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

package models

import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{Format, JsResult, JsValue, Json, OFormat}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Constants._

sealed trait RegistrationResponse

case class RegistrationTrnResponse(trn: String) extends RegistrationResponse

object RegistrationTrnResponse {
  implicit val formats: OFormat[RegistrationTrnResponse] = Json.format[RegistrationTrnResponse]
}

case class RegistrationFailureResponse(status: Int, code: String="", message: String="") extends RegistrationResponse

object RegistrationFailureResponse {
  implicit val formats: OFormat[RegistrationFailureResponse] = Json.format[RegistrationFailureResponse]
}

object AlreadyRegisteredResponse
  extends RegistrationFailureResponse(FORBIDDEN, ALREADY_REGISTERED_CODE, ALREADY_REGISTERED_ESTATE_MESSAGE)

object NoMatchResponse
  extends RegistrationFailureResponse(FORBIDDEN, NO_MATCH_CODE, NO_MATCH_MESSAGE)

object RegistrationResponse extends Logging {

  implicit object RegistrationResponseFormats extends Format[RegistrationResponse] {

    override def reads(json: JsValue): JsResult[RegistrationResponse] = json.validate[RegistrationTrnResponse]

    override def writes(o: RegistrationResponse): JsValue = o match {
      case x : RegistrationTrnResponse => Json.toJson(x)(RegistrationTrnResponse.formats)
      case x : RegistrationFailureResponse => Json.toJson(x)(RegistrationFailureResponse.formats)
    }

  }

  implicit lazy val httpReads: HttpReads[RegistrationResponse] =
    new HttpReads[RegistrationResponse] {
      override def read(method: String, url: String, response: HttpResponse): RegistrationResponse = {
        logger.info(s"Response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.as[RegistrationTrnResponse]
          case FORBIDDEN =>
            response.body match {
              case x if x.contains(ALREADY_REGISTERED_CODE) =>
                logger.info(s"Already registered response from des.")
                AlreadyRegisteredResponse
              case x if x.contains(NO_MATCH_CODE) =>
                logger.info(s"No match response from des.")
                NoMatchResponse
              case _ =>
                logger.error("Forbidden response from des.")
                RegistrationFailureResponse(response.status)
            }
          case _ => RegistrationFailureResponse(response.status)
        }
      }
    }

}
