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

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.estates.models.register.RegistrationDeclaration
import uk.gov.hmrc.estates.models.requests.IdentifierRequest
import uk.gov.hmrc.estates.models.{EstateRegistration, NameType, RegistrationResponse}
import uk.gov.hmrc.estates.repositories.TransformationRepository
import uk.gov.hmrc.estates.services.{AuditService, DesService}
import uk.gov.hmrc.estates.transformers.{ComposedDeltaTransform, DeclarationTransformer}

import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject()(repository: TransformationRepository,
                                    desService: DesService,
                                    auditService: AuditService,
                                    declarationTransformer: DeclarationTransformer
                                   )(implicit ec: ExecutionContext) {

  def submit(declaration: RegistrationDeclaration)
            (implicit request: IdentifierRequest[_]): Future[RegistrationResponse] = {

    repository.get(request.identifier) flatMap {
      case Some(transforms) =>
        buildSubmissionFromTransforms(declaration.name, transforms) match {
          case JsSuccess(json, _) =>
            json.asOpt[EstateRegistration] match {
              case Some(payload) =>
                desService.registerEstate(payload)
              case None =>
                Future.failed(new RuntimeException("Unable to parse transformed json as EstateRegistration"))
            }
          case JsError(errors) =>
            Future.failed(new RuntimeException(s"Unable to build json from transforms $errors"))
        }
      case None =>
        Future.failed(new RuntimeException("Unable to submit registration due to there being no transforms"))
    }
  }

  def buildSubmissionFromTransforms(name: NameType, transforms: ComposedDeltaTransform)
                                   (implicit request: IdentifierRequest[_]): JsResult[JsValue] = {
    val startingDocument = Json.obj()
    for {
      initial <- {
        Logger.info(s"[RegistrationService] applying transformations")
        transforms.applyTransform(startingDocument)
      }
      transformed <- {
        Logger.info(s"[RegistrationService] applying declaration transformations")
        transforms.applyDeclarationTransform(initial)
      }
      result <- {
        Logger.info(s"[RegistrationService] applying final transformations")
        declarationTransformer.transform(request.affinityGroup, transformed, name)
      }
    } yield result
  }


}
