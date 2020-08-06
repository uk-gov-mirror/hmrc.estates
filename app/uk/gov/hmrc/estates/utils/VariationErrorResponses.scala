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

package uk.gov.hmrc.estates.utils

import uk.gov.hmrc.estates.models.ErrorResponse

object VariationErrorResponses {
  object InvalidNameErrorResponse extends ErrorResponse("INVALID_NAME", "Provided name is invalid.")
  object InvalidUtrErrorResponse extends ErrorResponse("INVALID_UTR", "Provided utr is invalid.")
  object InvalidPostcodeErrorResponse extends ErrorResponse("INVALID_POSTCODE", "Provided postcode is invalid.")

  object EtmpDataStaleErrorResponse extends ErrorResponse("ETMP_DATA_STALE", "ETMP returned a changed form bundle number for the estate.")

  object InvalidRequestErrorResponse extends ErrorResponse("BAD_REQUEST", "Provided request is invalid.")
  object InvalidCorrelationIdErrorResponse extends ErrorResponse("INVALID_CORRELATIONID", "Submission has not passed validation. Invalid CorrelationId.")
  object DuplicateSubmissionErrorResponse extends ErrorResponse("DUPLICATE_SUBMISSION", "Duplicate Correlation Id was submitted.")
  object InternalServerErrorErrorResponse extends ErrorResponse("INTERNAL_SERVER_ERROR", "Internal server error.")
  object ServiceUnavailableErrorResponse extends ErrorResponse("SERVICE_UNAVAILABLE", "Service unavailable.")
}
