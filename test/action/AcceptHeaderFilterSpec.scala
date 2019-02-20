/*
 * Copyright 2019 HM Revenue & Customs
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

package action

import play.api.mvc.{RequestHeader, Result}
import play.api.test.FakeRequest
import support.BaseSpec
import uk.gov.hmrc.agentauthorisation.actions.AcceptHeaderFilter
import play.api.mvc.Results._

import scala.concurrent.Future

class AcceptHeaderFilterSpec extends BaseSpec {

  case class TestAcceptHeaderFilter(supportedVersion: Seq[String]) extends AcceptHeaderFilter(supportedVersion) {
    def response(f: RequestHeader => Future[Result])(rh: RequestHeader) = await(super.apply(f)(rh))
  }

  object TestAcceptHeaderFilter {

    val testHeaderVersion: String => Seq[(String, String)] =
      (testVersion: String) => Seq("Accept" -> s"application/vnd.hmrc.$testVersion+json")

    def fakeHeaders(headers: Seq[(String, String)]) = testRequest(FakeRequest().withHeaders(headers: _*))

    def toResult(result: Result) = (_: RequestHeader) => Future.successful(result)
  }

  import TestAcceptHeaderFilter._

  "AcceptHeaderFilter" should {
    "return None" when {
      "no errors found in request" in {
        val supportedVersions: Seq[String] = Seq("1.0")
        val fakeTestHeader = fakeHeaders(testHeaderVersion("1.0"))
        TestAcceptHeaderFilter(supportedVersions).response(toResult(Ok("")))(fakeTestHeader) shouldBe Ok("")
      }
    }

    "return Some" when {
      "request had no Accept Header" in {
        val supportedVersions: Seq[String] = Seq("1.0")
        val fakeTestHeader = fakeHeaders(Seq.empty)
        val result = TestAcceptHeaderFilter(supportedVersions).response(toResult(Ok))(fakeTestHeader)
        bodyOf(result) shouldBe """{"code":"ACCEPT_HEADER_INVALID","message":"Missing 'Accept' header."}"""
      }

      "request had an invalid Accept Header" in {
        val supportedVersions: Seq[String] = Seq("1.0")
        val fakeTestHeader = fakeHeaders(Seq("Accept" -> s"InvalidHeader"))
        val result = TestAcceptHeaderFilter(supportedVersions).response(toResult(Ok))(fakeTestHeader)
        bodyOf(result) shouldBe """{"code":"ACCEPT_HEADER_INVALID","message":"Invalid 'Accept' header."}"""
      }

      "request used an unsupported version" in {
        val supportedVersions: Seq[String] = Seq("1.0")
        val fakeTestHeader = fakeHeaders(testHeaderVersion("0.0"))
        val result = TestAcceptHeaderFilter(supportedVersions).response(toResult(Ok))(fakeTestHeader)
        bodyOf(result) shouldBe """{"code":"BAD_REQUEST","message":"Missing or unsupported version number."}"""
      }

      "request used an unsupported content-type" in {
        val supportedVersions: Seq[String] = Seq("1.0")
        val fakeTestHeader = fakeHeaders(Seq("Accept" -> s"application/vnd.hmrc.1.0+xml"))
        val result = TestAcceptHeaderFilter(supportedVersions).response(toResult(Ok))(fakeTestHeader)
        bodyOf(result) shouldBe """{"code":"BAD_REQUEST","message":"Missing or unsupported content-type."}"""
      }
    }
  }
}