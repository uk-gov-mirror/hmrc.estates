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

package controllers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import controllers.actions.IdentifierAction
import javax.inject.Inject
import models.register.RegistrationDeclaration
import models.{AlreadyRegisteredResponse, RegistrationTrnResponse}
import play.api.Logging
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.RosmPatternService
import services.register.RegistrationService
import utils.ErrorResults._
import utils.Session

import scala.concurrent.{ExecutionContext, Future}

class RegisterEstateController @Inject()(identifierAction: IdentifierAction,
                                         registrationService: RegistrationService,
                                         rosmPatternService: RosmPatternService)
                                        (implicit ec: ExecutionContext, cc: ControllerComponents
                                        ) extends EstatesBaseController(cc) with Logging {

  def register(): Action[JsValue] = identifierAction.async(parse.json) {
    implicit request => {
      request.body.validate[RegistrationDeclaration].fold(
        errors => {
          logger.error(s"[register][Session ID: ${Session.id(hc)}]" +
            s" unable to parse json as RegistrationDeclaration, $errors")
          Future.successful(BadRequest)
        },
        declaration => {
          registrationService
            .submit(declaration)
            .flatMap {
              case response @ RegistrationTrnResponse(trn) =>
                rosmPatternService.enrol(trn, request.affinityGroup, request.identifier) map { _ =>
                    Ok(Json.toJson(response))
                }
              case AlreadyRegisteredResponse =>
                logger.info(s"[register][Session ID: ${Session.id(hc)}]" +
                  s" unable to register estate for session due to it already being registered")
                Future.successful(duplicateSubmissionErrorResult)
              case e =>
                logger.warn(s"[register][Session ID: ${Session.id(hc)}]" +
                  s" unable to register estate for session due to $e")
                Future.successful(internalServerErrorErrorResult)
            }
        } recover {
          case e =>
            logger.warn(s"[register][Session ID: ${Session.id(hc)}]" +
              s" unable to register estate for session due exception ${e.getMessage}")
            internalServerErrorErrorResult
        }
      )
    }
  }

  def get(): Action[AnyContent] = identifierAction.async {
    implicit request =>

      registrationService
        .getRegistration()
        .map(response => Ok(Json.toJson(response)))
        .recover {
          case _ => internalServerErrorErrorResult
        }
  }

  def pdf(): Action[AnyContent] = identifierAction.async {
    implicit request =>

      val lengthFromHeader = 20000L

//      val outputStream = new java.io.ByteArrayOutputStream()
//      val bufferedOutputStream = new java.io.BufferedOutputStream(outputStream, 1024).write(Array(1, 2, 3).map(_.toByte))

//      bufferedOutputStream

//      val source = Source.fromIterator(bufferedOutputStream)


//      val source = new java.util.Array[Byte](lengthFromHeader)
//      source.

      val source = Source(List(ByteString.apply(Array("").map(_.toByte))))

      Future.successful(Result(
        header = play.api.mvc.ResponseHeader(
          status = OK,
          headers = Map(
            "Content-Disposition" -> "inline; filename.pdf",
            "Content-Type" -> "application/json",
            "Content-Length" -> ""
          )
        ),
        body = HttpEntity.Streamed(
          data = source,
          contentLength = Some(lengthFromHeader),
          contentType = Some("application/pdf")
        )
      ))
  }

}
