package org.hyperscala

import java.io.{File, IOException}
import java.util.Date
import java.util.concurrent.TimeUnit

import com.outr.scribe.Logging
import io.undertow.io.IoCallback
import io.undertow.server.handlers.ResponseCodeHandler
import io.undertow.server.handlers.cache.ResponseCache
import io.undertow.server.handlers.resource.{ClassPathResourceManager, DirectoryUtils, FileResource, FileResourceManager, RangeAwareResource, Resource, ResourceChangeEvent, ResourceChangeListener, ResourceHandler, ResourceManager, URLResource}
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{ByteRange, CanonicalPathUtils, DateUtils, ETagUtils, Headers, Methods, StatusCodes}
import org.xnio.{FileChangeCallback, FileChangeEvent, OptionMap, Xnio}

import scala.collection.JavaConverters._

class FunctionalResourceManager(val server: Server) extends ResourceManager {
  private var listeners = Set.empty[ResourceChangeListener]
  private var _mappings = Set.empty[ResourceMapping]

  private val defaultURLConversion: URL => Option[ClassPathResourceInfo] = (url: URL) => {
    Some(ClassPathResourceInfo(url.path))
  }

  private def defaultFileConversion(directory: File): URL => Option[FileResourceInfo] = (url: URL) => {
    val f = new File(directory, url.path)
    if (f.exists()) {
      Some(FileResourceInfo(f))
    } else {
      None
    }
  }

  override def removeResourceChangeListener(listener: ResourceChangeListener): Unit = synchronized {
    listeners -= listener
  }

  override def registerResourceChangeListener(listener: ResourceChangeListener): Unit = synchronized {
    listeners += listener
  }

  override def isResourceChangeListenerSupported: Boolean = true

  override def getResource(path: String): Resource = throw new UnsupportedOperationException("getResource should not be called directly. lookup should be used instead.")

  def lookup(url: URL): Option[ResourceResult] = _mappings.toStream.flatMap(_.lookup(url)).headOption

  def file(directory: File, addNow: Boolean = true)(conversion: URL => Option[FileResourceInfo] = defaultFileConversion(directory)): ResourceMapping = {
    val canonicalBase = directory.getCanonicalPath

    val resourceMapping = new PathResourceMapping {
      override def base: String = canonicalBase

      override def lookup(url: URL): Option[ResourceResult] = conversion(url).flatMap { info =>
        val file = info match {
          case ExplicitFileResourceInfo(f, attachment) => f
          case PathFileResourceInfo(path, attachment) => new File(directory, path)
        }
        if (file.exists()) {
          Some(ResourceResult(new FileResource(file, fileResourceManager, file.getCanonicalPath.substring(base.length)), info.attachment))
        } else {
          None
        }
      }
    }
    if (addNow) {
      add(resourceMapping)
    }
    resourceMapping
  }
  def classPath(basePath: String, addNow: Boolean = true)(conversion: URL => Option[ClassPathResourceInfo] = defaultURLConversion): ResourceMapping = {
    val properBase = if (basePath.endsWith("/")) {
      basePath.substring(1)
    } else {
      basePath
    }

    val resourceMapping = new ClassPathResourceMapping {
      override def base: String = properBase

      override def lookup(url: URL): Option[ResourceResult] = conversion(url).flatMap {
        case ExplicitClassPathResourceInfo(u, attachment) => Some(u)
        case PathClassPathResourceInfo(path, attachment) => Option(getClass.getClassLoader.getResource(s"$base$path"))
      }.map(u => ResourceResult(new URLResource(u, u.openConnection(), u.toString)))
    }
    if (addNow) {
      add(resourceMapping)
    }
    resourceMapping
  }

  def add(mapping: ResourceMapping): ResourceMapping = synchronized {
    _mappings += mapping
    mapping
  }

  def remove(mapping: ResourceMapping): Unit = synchronized {
    _mappings -= mapping
  }

  override def close(): Unit = {}

  protected[hyperscala] def fire(events: java.util.Collection[ResourceChangeEvent]): Unit = {
    listeners.foreach(_.handleChanges(events))
  }
}

sealed trait ResourceInfo {
  def attachment: Option[String]
}

sealed trait FileResourceInfo extends ResourceInfo

object FileResourceInfo {
  def apply(file: File): FileResourceInfo = ExplicitFileResourceInfo(file, None)
  def apply(path: String): FileResourceInfo = PathFileResourceInfo(path, None)
  def apply(file: File, attachment: String): FileResourceInfo = ExplicitFileResourceInfo(file, Some(attachment))
  def apply(path: String, attachment: String): FileResourceInfo = PathFileResourceInfo(path, Some(attachment))
}

case class ExplicitFileResourceInfo(file: File, attachment: Option[String]) extends FileResourceInfo
case class PathFileResourceInfo(path: String, attachment: Option[String]) extends FileResourceInfo

sealed trait ClassPathResourceInfo extends ResourceInfo

object ClassPathResourceInfo {
  def apply(url: java.net.URL): ClassPathResourceInfo = ExplicitClassPathResourceInfo(url, None)
  def apply(path: String): ClassPathResourceInfo = PathClassPathResourceInfo(path, None)
  def apply(url: java.net.URL, attachment: String): ClassPathResourceInfo = ExplicitClassPathResourceInfo(url, Some(attachment))
  def apply(path: String, attachment: String): ClassPathResourceInfo = PathClassPathResourceInfo(path, Some(attachment))
}

case class ExplicitClassPathResourceInfo(url: java.net.URL, attachment: Option[String]) extends ClassPathResourceInfo
case class PathClassPathResourceInfo(path: String, attachment: Option[String]) extends ClassPathResourceInfo

case class ResourceResult(resource: Resource, attachment: Option[String] = None)

trait ResourceMapping {
  def init(resourceManager: FunctionalResourceManager): Unit = {}

  def lookup(url: URL): Option[ResourceResult]
}

trait ClassPathResourceMapping extends ResourceMapping {
  protected lazy val classPathResourceManager = new ClassPathResourceManager(getClass.getClassLoader)

  def base: String
}

trait PathResourceMapping extends ResourceMapping {
  private lazy val fileSystemWatcher = Xnio.getInstance().createFileSystemWatcher(s"Watcher for $base", OptionMap.EMPTY)
  protected lazy val fileResourceManager = new FileResourceManager(new File(base), 100L)

  def base: String

  override def init(resourceManager: FunctionalResourceManager): Unit = {
    super.init(resourceManager)

    fileSystemWatcher.watchPath(new File(base), new FileChangeCallback {
      override def handleChanges(changes: java.util.Collection[FileChangeEvent]): Unit = resourceManager.synchronized {
        val events = changes.asScala.collect {
          case change if change.getFile.getAbsolutePath.startsWith(base) => {
            val path = change.getFile.getAbsolutePath.substring(base.length)
            new ResourceChangeEvent(path, ResourceChangeEvent.Type.valueOf(change.getType.name()))
          }
        }.toList
        if (events.nonEmpty) {
          resourceManager.fire(events.asJava)
        }
      }
    })
  }
}

class FunctionalResourceHandler(resourceManager: FunctionalResourceManager) extends ResourceHandler(resourceManager) with Handler with Logging {
  setDirectoryListingEnabled(false)

  override def isURLMatch(url: URL): Boolean = resourceManager.lookup(url).nonEmpty

  override def handleRequest(url: URL, exchange: HttpServerExchange): Unit = handleRequest(exchange)

  override def priority: Priority = Priority.Low

  override def handleRequest(exchange: HttpServerExchange): Unit = exchange.getRequestMethod match {
    case Methods.GET | Methods.POST => serveResource(exchange, sendContent = true)
    case Methods.HEAD => serveResource(exchange, sendContent = false)
    case _ => {
      exchange.setStatusCode(StatusCodes.METHOD_NOT_ALLOWED)
      exchange.endExchange()
    }
  }

  private def serveResource(exchange: HttpServerExchange, sendContent: Boolean): Unit = {
    if (DirectoryUtils.sendRequestedBlobs(exchange)) {
      // Support for directory listing (possible future usage)
    } else if (!getAllowed.resolve(exchange)) {   // TODO: support forbidden in functional
      exchange.setStatusCode(StatusCodes.FORBIDDEN)
      exchange.endExchange()
    } else {
      val cache = exchange.getAttachment(ResponseCache.ATTACHMENT_KEY)
      val cachable = getCachable.resolve(exchange)

      if (cachable && getCacheTime != null) {
        exchange.getResponseHeaders.put(Headers.CACHE_CONTROL, s"public, max-age=$getCacheTime")
        val date = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(getCacheTime.toLong)
        val dateHeader = DateUtils.toDateString(new Date(date))
        exchange.getResponseHeaders.put(Headers.EXPIRES, dateHeader)
      }
      if (cache != null && cachable && cache.tryServeResponse()) {
        // Cached response
      } else {
        val serverSession = Server.session.undertowSession()
        val dispatchTask = new HttpHandler {
          override def handleRequest(exchange: HttpServerExchange): Unit = try {
            Server.withServerSession(serverSession) {
              var url = exchange.url
              if ((File.separatorChar == '/' || !exchange.getRelativePath.contains(File.separator)) && isCanonicalizePaths) {
                url = url.copy(path = CanonicalPathUtils.canonicalize(exchange.getRelativePath))
              }
              val resourceResultOption: Option[ResourceResult] = resourceManager.lookup(url)
              resourceResultOption match {
                case Some(resourceResult) => {
                  val resource = resourceResult.resource
                  if (resource.isDirectory || exchange.getRelativePath.endsWith("/")) {
                    exchange.setStatusCode(StatusCodes.NOT_FOUND)
                    exchange.endExchange()
                  } else {
                    resourceResult.attachment.foreach { fileName =>
                      exchange.getResponseHeaders.put(Headers.CONTENT_DISPOSITION, s"""attachment; filename="$fileName"""")
                    }

                    val etag = resource.getETag
                    val lastModified = resource.getLastModified
                    if (!ETagUtils.handleIfMatch(exchange, etag, false) || !DateUtils.handleIfUnmodifiedSince(exchange, lastModified)) {
                      exchange.setStatusCode(StatusCodes.NOT_MODIFIED)
                      exchange.endExchange()
                    } else {
                      val contentEncodedResourceManager = getContentEncodedResourceManager
                      val contentLength = resource.getContentLength
                      if (contentLength != null && !exchange.getResponseHeaders.contains(Headers.TRANSFER_ENCODING)) {
                        exchange.setResponseContentLength(contentLength)
                      }
                      val rangeResponse = resource match {
                        case rar: RangeAwareResource if rar.isRangeSupported && contentLength != null && contentEncodedResourceManager == null => {
                          exchange.getResponseHeaders.put(Headers.ACCEPT_RANGES, "bytes")
                          val range = ByteRange.parse(exchange.getRequestHeaders.getFirst(Headers.RANGE))
                          if (range != null && range.getRanges == 1 && resource.getContentLength != null) {
                            Option(range.getResponseResult(resource.getContentLength, exchange.getRequestHeaders.getFirst(Headers.IF_RANGE), resource.getLastModified, Option(resource.getETag).map(_.getTag).orNull))
                          } else {
                            None
                          }
                        }
                        case _ => None
                      }
                      val (start, end) = rangeResponse.map { rr =>
                        exchange.setStatusCode(rr.getStatusCode)
                        exchange.getResponseHeaders.put(Headers.CONTENT_RANGE, rr.getContentRange)
                        exchange.setResponseContentLength(rr.getContentLength)
                        (rr.getStart, rr.getEnd)
                      }.getOrElse((-1L, -1L))
                      if (exchange.getStatusCode != StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE) {
                        if (!exchange.getResponseHeaders.contains(Headers.CONTENT_TYPE)) {
                          val contentType = Option(resource.getContentType(getMimeMappings)).getOrElse("application/octet/stream")
                          exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, contentType)
                        }
                        if (lastModified != null) {
                          exchange.getResponseHeaders.put(Headers.LAST_MODIFIED, resource.getLastModifiedString)
                        }
                        if (etag != null) {
                          exchange.getResponseHeaders.put(Headers.ETAG, etag.toString)
                        }
                        Option(contentEncodedResourceManager).map(_.getResource(resource, exchange)) match {
                          case Some(encoded) => {
                            exchange.getResponseHeaders.put(Headers.CONTENT_ENCODING, encoded.getContentEncoding)
                            exchange.getResponseHeaders.put(Headers.CONTENT_LENGTH, encoded.getResource.getContentLength)
                            encoded.getResource.serve(exchange.getResponseSender, exchange, IoCallback.END_EXCHANGE)
                          }
                          case None if !sendContent => exchange.endExchange()
                          case None => rangeResponse match {
                            case Some(rr) => resource.asInstanceOf[RangeAwareResource].serveRange(exchange.getResponseSender, exchange, start, end, IoCallback.END_EXCHANGE)
                            case None => resource.serve(exchange.getResponseSender, exchange, IoCallback.END_EXCHANGE)
                          }
                        }
                      }
                    }
                  }
                }
                case None => ResponseCodeHandler.HANDLE_404.handleRequest(exchange)
              }
            }
          } catch {
            case exc: IOException => {
              resourceManager.server.error(exc)
              exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR)
              exchange.endExchange()
            }
          }
        }
        if (exchange.isInIoThread) {
          exchange.dispatch(dispatchTask)
        } else {
          dispatchTask.handleRequest(exchange)
        }
      }
    }
  }
}