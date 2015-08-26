package com.mle.play

import java.nio.file.{Files, Path, Paths}

import com.mle.file.FileUtilities
import com.mle.security.KeyStores
import com.mle.util.{Log, Util, Utils}
import play.{Routes, DefaultApplication}
import play.api.routing.Router
import play.api.{Environment, Play, DefaultApplication, Mode}

//import play.core.StaticApplication

import play.core.server.{ServerConfig, NettyServer}

/**
 * Starts Play Framework 2, does not create a RUNNING_PID file.
 *
 * An alternative to the official ways to start Play,
 * this integrates better with my more generic init scripts.
 *
 * @author mle
 */
trait PlayLifeCycle extends KeyStores with Log {

  protected val (httpPortKey, httpsPortKey, httpAddressKey) =
    ("http.port", "https.port", "http.address")
  protected val defaultHttpPort = 9000
  protected val defaultHttpAddress = "0.0.0.0"

  var nettyServer: Option[NettyServer] = None

  def isHttpAvailable = tryReadInt(httpPortKey).isDefined

  def isHttpsAvailable = tryReadInt(httpsPortKey).isDefined

  def appName: String

  def start(rs: Router.Routes) {
    FileUtilities.basePath = Paths get sys.props.get(s"$appName.home").getOrElse(sys.props("user.dir"))
    log info s"Starting $appName... app home: ${FileUtilities.basePath}"
    sys.props ++= conformize(readConfFile(appName))
    validateKeyStoreIfSpecified()

    /**
     * NettyServer.createServer insists on writing a RUNNING_PID file.
     * Fuck that.
     */
    nettyServer = Some(createServer(rs))
  }

  protected def tryReadInt(key: String) =
    sys.props.get(key).filter(_ != "disabled").flatMap(ps => Utils.opt[Int, NumberFormatException](Integer.parseInt(ps)))

  protected def createServer(rs: Router.Routes): NettyServer = {
    val serverConfig = ServerConfig(
      classLoader = this.getClass.getClassLoader,
      rootDir = FileUtilities.basePath.toFile,
      port = tryReadInt(httpPortKey),
      sslPort = tryReadInt(httpsPortKey),
      address = sys.props.get(httpAddressKey) getOrElse defaultHttpAddress)
    val server = NettyServer.fromRouter(serverConfig)(rs)
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

  private def propsFromFile(file: Path): Map[String, String] = {
    if (Files.exists(file)) Util.props(file)
    else Map.empty[String, String]
  }
}
