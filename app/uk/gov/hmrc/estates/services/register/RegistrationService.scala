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
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue, Json}
import play.api.mvc.AnyContent
import uk.gov.hmrc.estates.models.{EstateRegistration, NameType, RegistrationFailureResponse, RegistrationResponse}
import uk.gov.hmrc.estates.models.register.RegistrationDeclaration
import uk.gov.hmrc.estates.models.requests.IdentifierRequest
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
            (implicit request: IdentifierRequest[_]) : Future[RegistrationResponse] = ???

  def buildSubmissionFromTransforms(name: NameType, transforms : ComposedDeltaTransform)
                                   (implicit request: IdentifierRequest[_]): JsResult[JsValue] = {

        // Audit transforms to splunk

        // Audit final document about to be submitted

        // Recover and audit why we couldn't submit the registration

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
