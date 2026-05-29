package org.example.core.adapter.web.impl.sttp

import org.example.core.adapter.web.impl.sttp.model.BackendType
import org.example.core.adapter.web.impl.sttp.model.BackendType.{Default, UnsafeSSL}
import sttp.client4.httpclient.HttpClientSyncBackend
import sttp.client4.{DefaultSyncBackend, SyncBackend}

import java.net.http.HttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}

object STTPBackendFactory {


  def create(backendType: BackendType): SyncBackend = backendType match {
    case Default => DefaultSyncBackend()
    case UnsafeSSL => createUnsafeBackend()
  }


  private def createUnsafeBackend(): SyncBackend = {
    val sslContext = SSLContext.getInstance("TLS")
    val trustAllCerts = Array[TrustManager](new X509TrustManager {
      override def getAcceptedIssuers: Array[X509Certificate] = Array.empty

      override def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = {}

      override def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = {}
    })
    sslContext.init(null, trustAllCerts, new SecureRandom())

    val client = HttpClient.newBuilder()
      .sslContext(sslContext)
      .build()

    HttpClientSyncBackend.usingClient(client)
  }
}
