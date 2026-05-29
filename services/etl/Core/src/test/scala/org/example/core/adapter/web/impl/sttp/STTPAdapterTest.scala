package org.example.core.adapter.web.impl.sttp

import org.example.core.adapter.web.impl.sttp.model._
import org.example.core.config.model.structures.NetworkConf
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client4.SyncBackend
import sttp.client4.testing.SyncBackendStub
import sttp.model.StatusCode

class STTPAdapterTest extends AnyFlatSpec with Matchers {

  private val conf = NetworkConf(
    headers = Array("Authorization" -> "Bearer token"),
    requestsPS = 10,
    timeout = 5000
  )

  private def adapterWith(backend: SyncBackend): STTPAdapter =
    new STTPAdapter(conf, () => backend)

  private def stubWith(statusCode: Int, body: String): SyncBackend =
    SyncBackendStub.whenAnyRequest
      .thenRespondAdjust(body, StatusCode(statusCode))

  "STTPAdapter.execute" should "return WebResponse on 200" in {
    val adapter = adapterWith(stubWith(200, """{"ok":true}"""))
    val result = adapter.execute("https://example.com/api")

    result shouldBe a[Right[_, _]]
    result.toOption.get.statusCode shouldBe 200
    result.toOption.get.body shouldBe """{"ok":true}"""
  }

  it should "return HttpError on 404" in {
    val adapter = adapterWith(stubWith(404, "Not Found"))
    val result = adapter.execute("https://example.com/api")

    result shouldBe a[Left[_, _]]
    result.left.toOption.get shouldBe a[HttpError]
    result.left.toOption.get.asInstanceOf[HttpError].statusCode shouldBe 404
  }

  it should "return HttpError on 500" in {
    val adapter = adapterWith(stubWith(500, "Server Error"))
    val result = adapter.execute("https://example.com/api")

    result.left.toOption.get shouldBe a[HttpError]
    result.left.toOption.get.asInstanceOf[HttpError].statusCode shouldBe 500
  }

  it should "include response headers in WebResponse" in {
    val backend = SyncBackendStub.whenAnyRequest
      .thenRespondAdjust("body", StatusCode.Ok)

    val adapter = adapterWith(backend)
    val result = adapter.execute("https://example.com/api")

    result shouldBe a[Right[_, _]]
    result.toOption.get.headers should not be null
  }

  it should "return ConnectionError on network exception" in {
    val throwingBackend = SyncBackendStub.whenAnyRequest
      .thenThrow(new RuntimeException("connection refused"))

    val adapter = adapterWith(throwingBackend)
    val result = adapter.execute("https://example.com/api")

    result.left.toOption.get shouldBe a[ConnectionError]
  }

  "STTPAdapter.readBody" should "return body string on success" in {
    val adapter = adapterWith(stubWith(200, "response body"))
    adapter.readBody("https://example.com") shouldBe Right("response body")
  }

  it should "return Left on error status" in {
    val adapter = adapterWith(stubWith(403, "Forbidden"))
    adapter.readBody("https://example.com") shouldBe a[Left[_, _]]
  }

  "STTPAdapter.readBodyOrNone" should "return Some on success" in {
    val adapter = adapterWith(stubWith(200, "data"))
    adapter.readBodyOrNone("https://example.com") shouldBe Some("data")
  }

  it should "return None on failure" in {
    val adapter = adapterWith(stubWith(500, "err"))
    adapter.readBodyOrNone("https://example.com") shouldBe None
  }

  "STTPAdapter.readBodyOrThrow" should "return body string on success" in {
    val adapter = adapterWith(stubWith(200, "payload"))
    adapter.readBodyOrThrow("https://example.com") shouldBe "payload"
  }

  it should "throw RuntimeException on HTTP error" in {
    val adapter = adapterWith(stubWith(401, "Unauthorized"))
    intercept[RuntimeException] {
      adapter.readBodyOrThrow("https://example.com")
    }
  }

  it should "wrap ConnectionError cause in RuntimeException" in {
    val cause = new RuntimeException("timeout")
    val throwingBackend = SyncBackendStub.whenAnyRequest.thenThrow(cause)

    val adapter = adapterWith(throwingBackend)
    val ex = intercept[RuntimeException] {
      adapter.readBodyOrThrow("https://example.com")
    }

    ex.getCause shouldBe cause
  }
}
