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
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.models.register.RegistrationDeclaration
import uk.gov.hmrc.estates.models.requests.IdentifierRequest
import uk.gov.hmrc.estates.repositories.TransformationRepository
import uk.gov.hmrc.estates.services.{AuditService, DesService}
import uk.gov.hmrc.estates.transformers.ComposedDeltaTransform
import uk.gov.hmrc.estates.transformers.register.DeclarationTransform
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject()(repository: TransformationRepository,
                                    desService: DesService,
                                    declarationTransformer: DeclarationTransform,
                                    auditService: AuditService
                                   )(implicit ec: ExecutionContext) {

  def getRegistration()(implicit request: IdentifierRequest[_], hc: HeaderCarrier): Future[EstateRegistrationNoDeclaration] = {

    repository.get(request.identifier) flatMap {
      case Some(transforms) =>
        buildPrintFromTransforms(transforms) match {
          case JsSuccess(json, _) =>
            json.asOpt[EstateRegistrationNoDeclaration] match {
              case Some(payload) =>

                auditService.auditGetRegistrationSuccess(payload)

                Future.successful(payload)
              case None =>
                val reason = "Unable to parse transformed json as EstateRegistrationNoDeclaration"
                auditService.auditGetRegistrationFailed(transforms, reason)
                Future.failed(new RuntimeException(reason))
            }
          case JsError(errors) =>
            val reason = "Unable to build json from transforms"
            auditService.auditGetRegistrationFailed(transforms, reason, errors.toString)
            Future.failed(new RuntimeException(s"$reason: $errors"))
        }
      case None =>
        val reason = "Unable to get registration due to there being no transforms"
        auditService.auditGetRegistrationFailed(ComposedDeltaTransform(Seq.empty), reason)
        Future.failed(new RuntimeException(reason))
    }
  }

  def submit(declaration: RegistrationDeclaration)
            (implicit request: IdentifierRequest[_], hc: HeaderCarrier): Future[RegistrationResponse] = {

    repository.get(request.identifier) flatMap {
      case Some(transforms) =>

        buildSubmissionFromTransforms(declaration.name, transforms) match {
          case JsSuccess(json, _) =>

            json.asOpt[EstateRegistration] match {
              case Some(payload) =>
                submitAndAuditResponse(payload)
              case None =>
                Logger.warn(s"[RegistrationService] unable to send registration for session: ${hc.sessionId.map(_.value).getOrElse("")} due to being unable to validate as EstateRegistration")

                val reason = "Unable to parse transformed json as EstateRegistration"

                auditService.auditRegistrationTransformationError(
                  data = json,
                  transforms = Json.toJson(transforms),
                  errorReason = reason
                )
                Future.failed(new RuntimeException(reason))
            }
          case JsError(errors) =>

            Logger.warn(s"[RegistrationService] unable to build submission payload for session: ${hc.sessionId.map(_.value).getOrElse("")}")

            val reason = "Unable to build json from transforms"

            auditService.auditRegistrationTransformationError(
              transforms = Json.toJson(transforms),
              errorReason = reason,
              jsErrors = errors.toString()
            )
            Future.failed(new RuntimeException(s"$reason: $errors"))
        }
      case None =>

        Logger.warn(s"[RegistrationService] unable to send registration for session: ${hc.sessionId.map(_.value).getOrElse("")} due to there being no data in mongo")

        val reason = "Unable to submit registration due to there being no transforms"

        auditService.auditRegistrationTransformationError(errorReason = reason)

        Future.failed(new RuntimeException(reason))
    }
  }

  private def submitAndAuditResponse(payload: EstateRegistration)
                                    (implicit request: IdentifierRequest[_], hc: HeaderCarrier) : Future[RegistrationResponse] = {
    desService.registerEstate(payload) map {
      case r@RegistrationTrnResponse(trn) =>

        Logger.info(s"[RegistrationService] submission for session: ${hc.sessionId.map(_.value).getOrElse("")} received TRN $trn")

        auditService.auditRegistrationSubmitted(payload, trn)
        r
      case r: RegistrationFailureResponse =>

        Logger.error(s"[RegistrationService] submission for session: ${hc.sessionId.map(_.value).getOrElse("")} was unable to be submitted due to status ${r.status} ${r.code} and ${r.message}")

        auditService.auditRegistrationFailed(request.identifier, Json.toJson(payload), r)
        r
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
