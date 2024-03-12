package sttp.client4

import java.io.{File, InputStream}
import java.nio.file.Path
import sttp.client4.internal.SttpFile
import sttp.model.{Part, StatusCode}

trait SttpExtensions {

  /** Specify that the body should be passed as an input stream to the given function `f`. After the function completes,
    * the input stream is always closed.
    *
    * If the response code is not successful, the body is returned as a `String`.
    *
    * '''Warning:''' this type of responses is supported only by some backends on the JVM.
    */
  def asInputStream[T](f: InputStream => T): ResponseAs[Either[String, T]] =
    asEither(asStringAlways, asInputStreamAlways(f))

  /** Specify that the body should be passed as an input stream to the given function `f`. After the function completes,
    * the input stream is always closed.
    *
    * '''Warning:''' this type of responses is supported only by some backends on the JVM.
    */
  def asInputStreamAlways[T](f: InputStream => T): ResponseAs[T] = new ResponseAs(ResponseAsInputStream(f))

  /** Specify that the body should be returned as an input stream. It is the responsibility of the user to properly
    * close the stream.
    *
    * If the response code is not successful, the body is returned as a `String`.
    *
    * '''Warning:''' this type of responses is supported only by some backends on the JVM.
    */
  def asInputStreamUnsafe: ResponseAs[Either[String, InputStream]] = asEither(asStringAlways, asInputStreamAlwaysUnsafe)

  /** Specify that the body should be returned as an input stream. It is the responsibility of the user to properly
    * close the stream.
    *
    * '''Warning:''' this type of responses is supported only by some backends on the JVM.
    */
  def asInputStreamAlwaysUnsafe: ResponseAs[InputStream] = new ResponseAs(ResponseAsInputStreamUnsafe)

  def asFile(file: File): ResponseAs[Either[String, File]] = asEither(asStringAlways, asFileAlways(file))

  def asFileAlways(file: File): ResponseAs[File] =
    new ResponseAs(ResponseAsFile(SttpFile.fromFile(file)).map(_.toFile))

  def asPath(path: Path): ResponseAs[Either[String, Path]] = asEither(asStringAlways, asPathAlways(path))

  def asPathAlways(path: Path): ResponseAs[Path] =
    new ResponseAs(ResponseAsFile(SttpFile.fromPath(path)).map(_.toPath))

  /** Content type will be set to `application/octet-stream`, can be overridden later using the `contentType` method.
    *
    * File name will be set to the name of the file.
    */
  def multipartFile(name: String, data: File): Part[BasicBodyPart] = multipartSttpFile(name, SttpFile.fromFile(data))

  /** Content type will be set to `application/octet-stream`, can be overridden later using the `contentType` method.
    *
    * File name will be set to the name of the file.
    */
  def multipartFile(name: String, data: Path): Part[BasicBodyPart] = multipartSttpFile(name, SttpFile.fromPath(data))
}

object SttpExtensions {

  /** This needs to be platform-specific due to #1682, as on JS we don't get access to the 101 status code.
    * asWebSocketEither delegates to this method, as the method itself cannot be moved, due to binary compatibility.
    */
  private[client4] def asWebSocketEitherPlatform[F[_], A, B](
      onError: ResponseAs[A],
      onSuccess: WebSocketResponseAs[F, B]
  ): WebSocketResponseAs[F, Either[A, B]] =
    ws.async
      .fromMetadata(
        onError.map(Left(_)),
        ConditionalResponseAs(_.code == StatusCode.SwitchingProtocols, onSuccess.map(Right(_)))
      )
      .showAs(s"either(${onError.show}, ${onSuccess.show})")
}
