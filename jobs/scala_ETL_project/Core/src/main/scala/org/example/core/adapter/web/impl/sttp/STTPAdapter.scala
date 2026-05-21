package org.example.core.adapter.web.impl.sttp

import com.typesafe.scalalogging.LazyLogging
import org.example.core.adapter.web.WebAdapter
import org.example.core.adapter.web.impl.sttp.model._
import org.example.core.config.model.structures.NetworkConf
import sttp.client4.{SyncBackend, basicRequest}
import sttp.model.Uri

import java.util.concurrent.{Callable, Executors, TimeUnit, TimeoutException}
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}


class STTPAdapter(
                   conf: NetworkConf,
                   backendProvider: () => SyncBackend
                 ) extends WebAdapter with LazyLogging {

  @transient private lazy val backend: SyncBackend = backendProvider()

  @transient private lazy val httpPool = Executors.newCachedThreadPool()


  override def execute(url: String): Either[WebError, WebResponse] = {
    logger.info(s"Выполнение HTTP GET запроса к URL: $url")

    val uriEither = Uri.parse(url).left.map { e =>
      val msg = s"Невалидный URL: $e"
      logger.error(msg)
      ParsingError(msg)
    }

    uriEither.flatMap { uri =>
      val request = basicRequest
        .headers(conf.headers.toMap)
        .get(uri)
        .readTimeout(Duration(conf.timeout, MILLISECONDS))

      val callable: Callable[sttp.client4.Response[Either[String, String]]] =
        () => request.send(backend)
      val future = httpPool.submit(callable)

      val triedResponse = Try(future.get(conf.timeout.toLong + 3000, TimeUnit.MILLISECONDS))
        .recoverWith {
          case _: TimeoutException =>
            future.cancel(true)
            logger.error(s"HTTP таймаут (${conf.timeout + 3000}ms) при запросе к $url")
            Failure(new Exception(s"HTTP request timed out after ${conf.timeout + 3000}ms"))
        }

      triedResponse match {
        case Success(response) =>
          val bodyString = response.body match {
            case Right(b) => b
            case Left(b) => b
          }

          val webResponse = WebResponse(
            body = bodyString,
            statusCode = response.code.code,
            headers = response.headers.map(h => h.name -> h.value).toMap
          )

          if (response.code.isSuccess) {
            logger.debug(s"Успешный ответ от $url. Код статуса: ${response.code.code}")
            Right(webResponse)
          } else {
            logger.error(s"HTTP ошибка от $url. Код: ${response.code.code}. Тело: $bodyString")
            Left(HttpError(response.code.code, bodyString))
          }

        case Failure(exception) =>
          logger.error(s"Ошибка соединения при запросе к $url: ${exception.getMessage}", exception)
          Left(ConnectionError(exception))
      }
    }

  }

  override def readBody(url: String): Either[WebError, String] =
    execute(url).map(_.body)

  override def readBodyOrThrow(url: String): String =
    readBody(url) match {
      case Right(body) => body
      case Left(error) =>
        logger.error(s"Критическая ошибка при получении тела ответа $url: ${error.getMessage}")
        throw new RuntimeException(error.getMessage, error match {
          case ConnectionError(c) => c
          case _ => null
        })
    }

  override def readBodyOrNone(url: String): Option[String] =
    readBody(url).toOption


  override def close(): Unit = {
    logger.info("Закрытие STTP backend")
    try {
      backend.close()
    } catch {
      case NonFatal(e) => logger.error("Ошибка при закрытии STTP backend", e)
    }
    try {
      httpPool.shutdownNow()
    } catch {
      case NonFatal(e) => logger.error("Ошибка при закрытии HTTP thread pool", e)
    }
  }
}

object STTPAdapter {

  def apply(conf: NetworkConf, backendType: BackendType): STTPAdapter = {
    new STTPAdapter(conf, () => STTPBackendFactory.create(backendType))
  }

  def apply(conf: NetworkConf): STTPAdapter = {
    apply(conf, BackendType.Default)
  }
}