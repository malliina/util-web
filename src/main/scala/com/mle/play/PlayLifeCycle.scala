package com.mle.play

import play.core.server.NettyServer
import play.core.StaticApplication
import com.mle.util.{Log, FileUtilities, Util}
import java.nio.file.{Files, Path, Paths}
import scala.Some
import java.io.{FileInputStream, FileNotFoundException}
import java.security.KeyStore

/**
 * Starts Play Framework 2, does not create a RUNNING_PID file.
 *
 * An alternative to the official ways to start Play,
 * this integrates better with my more generic init scripts.
 *
 * @author mle
 */
trait PlayLifeCycle extends Log {

  protected val (httpPortKey, httpsPortKey, httpAddressKey) =
    ("http.port", "https.port", "http.address")
  protected val (keyStoreKey, keyStorePassKey, keyStoreTypeKey) =
    ("https.keyStore", "https.keyStorePassword", "https.keyStoreType")
  protected val defaultHttpPort = 9000
  protected val defaultHttpAddress = "0.0.0.0"
  protected val defaultKeyStoreType = "JKS"

  var nettyServer: Option[NettyServer] = None

  def appName: String

  def main(args: Array[String]) {
    // TODO add option to kill running instance gracefully using e.g. remote akka actors
    start()
  }

  def start() {
    FileUtilities.basePath = Paths get sys.props.get(s"$appName.home").getOrElse(sys.props("user.dir"))
    log info s"Starting $appName... app home: ${FileUtilities.basePath}"
    sys.props ++= conformize(readConfFile(appName))
    validateKeyStoreIfSpecified()

    /**
     * NettyServer.createServer insists on writing a RUNNING_PID file.
     * Fuck that.
     */
    nettyServer = Some(createServer())
  }

  private def createServer() = {
    val server = new NettyServer(
      new StaticApplication(FileUtilities.basePath.toFile),
      Option(System.getProperty(httpPortKey)).map(Integer.parseInt).orElse(Some(defaultHttpPort)),
      Option(System.getProperty(httpsPortKey)).map(Integer.parseInt),
      Option(System.getProperty(httpAddressKey)).getOrElse(defaultHttpAddress)
    )
    Util.addShutdownHook(server.stop())
    server
  }

  /**
   * Reads a file named `confNameWithoutExtension`.conf if it exists.
   *
   * @param confNameWithoutExtension name of conf, typically the name of the app
   * @return the key-value pairs from the conf file; an empty map if the file doesn't exist
   */
  def readConfFile(confNameWithoutExtension: String): Map[String, String] = {
    // adds settings in app conf to system properties
    val confFile = FileUtilities.pathTo(s"$confNameWithoutExtension.conf")
    if (Files.exists(confFile)) propsFromFile(confFile)
    else Map.empty[String, String]
  }

  /**
   * @param params key-value pairs
   * @return key-value pairs where key https.keyStore, if any, is an absolute path
   */
  def conformize(params: Map[String, String]): Map[String, String] = {
    params.get(keyStoreKey).map(keyStorePath => {
      val absKeyStorePath = FileUtilities.pathTo(keyStorePath).toAbsolutePath
      params.updated(keyStoreKey, absKeyStorePath.toString)
    }).getOrElse(params)
  }

  private def sysProp(key: String) = sys.props.get(key)

  private def verifyFileReadability(file: Path): Unit = {
    import Files._
    if (!exists(file)) {
      throw new FileNotFoundException(file.toString)
    }
    if (!isRegularFile(file)) {
      throw new Exception(s"Not a regular file: $file")
    }
    if (!isReadable(file)) {
      throw new Exception(s"File exists but is not readable: $file")
    }
  }

  private def validateKeyStoreIfSpecified(): Unit = {
    sysProp(keyStoreKey) foreach (keyStore => {
      val absPath = FileUtilities.pathTo(keyStore).toAbsolutePath
      verifyFileReadability(absPath)
      val pass = sysProp(keyStorePassKey) getOrElse (throw new Exception(s"Key $keyStoreKey exists but no corresponding $keyStorePassKey was found."))
      val storeType = sysProp(keyStoreTypeKey) getOrElse defaultKeyStoreType
      validateKeyStore(Paths get keyStore, pass, storeType)
    })
  }

  private def validateKeyStore(keyStore: Path, keyStorePassword: String, keyStoreType: String = defaultKeyStoreType): Unit = {
    val ks = KeyStore.getInstance(keyStoreType)
    Util.using(new FileInputStream(keyStore.toFile))(keyStream => ks.load(keyStream, keyStorePassword.toCharArray))
  }

  private def propsFromFile(file: Path): Map[String, String] = {
    if (Files.exists(file)) Util.props(file)
    else Map.empty[String, String]
  }
}
