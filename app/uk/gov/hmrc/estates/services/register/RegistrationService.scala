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
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.estates.models.register.RegistrationDeclaration
import uk.gov.hmrc.estates.models.requests.IdentifierRequest
import uk.gov.hmrc.estates.models.{EstateRegistration, EstateRegistrationNoDeclaration, NameType, RegistrationResponse}
import uk.gov.hmrc.estates.repositories.TransformationRepository
import uk.gov.hmrc.estates.services.DesService
import uk.gov.hmrc.estates.transformers.ComposedDeltaTransform
import uk.gov.hmrc.estates.transformers.register.DeclarationTransform

import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject()(repository: TransformationRepository,
                                    desService: DesService,
                                    declarationTransformer: DeclarationTransform
                                   )(implicit ec: ExecutionContext) {

  def getRegistration()(implicit request: IdentifierRequest[_]): Future[EstateRegistrationNoDeclaration] = {

    repository.get(request.identifier) flatMap {
      case Some(transforms) =>
        buildPrintFromTransforms(transforms) match {
          case JsSuccess(json, _) =>
            json.asOpt[EstateRegistrationNoDeclaration] match {
              case Some(payload) =>
                Future.successful(payload)
              case None =>
                Future.failed(new RuntimeException("Unable to parse transformed json as EstateRegistrationNoDeclaration"))
            }
          case JsError(errors) =>
            Future.failed(new RuntimeException(s"Unable to build json from transforms $errors"))
        }
      case None =>
        Future.failed(new RuntimeException("Unable to get registration due to there being no transforms"))
    }
  }

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

  private def buildPrintFromTransforms(transforms: ComposedDeltaTransform)
                                   (implicit request: IdentifierRequest[_]): JsResult[JsValue] = {
    for {
      result <- applyTransforms(transforms)
    } yield result
  }

  def buildSubmissionFromTransforms(name: NameType, transforms: ComposedDeltaTransform)
                                   (implicit request: IdentifierRequest[_]): JsResult[JsValue] = {
    for {
      transformsApplied <- applyTransforms(transforms)
      declarationTransformsApplied <- applyTransformsForDeclaration(transforms, transformsApplied)
      result <- applyDeclarationAddressTransform(declarationTransformsApplied, request.affinityGroup, name)
    } yield result
  }

  private def applyTransforms(transforms: ComposedDeltaTransform): JsResult[JsValue] = {
    Logger.info(s"[RegistrationService] applying transformations")
    transforms.applyTransform(Json.obj())
  }

  private def applyTransformsForDeclaration(transforms: ComposedDeltaTransform, original: JsValue): JsResult[JsValue] = {
    Logger.info(s"[RegistrationService] applying declaration transformations")
    transforms.applyDeclarationTransform(original)
  }

  private def applyDeclarationAddressTransform(original: JsValue,
                                               affinityGroup: AffinityGroup,
                                               name: NameType): JsResult[JsValue] = {
    Logger.info(s"[RegistrationService] applying declaration address transform")
    declarationTransformer.transform(affinityGroup, original, name)
  }

}
