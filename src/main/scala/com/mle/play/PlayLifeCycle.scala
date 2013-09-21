package com.mle.play

import play.core.server.NettyServer
import play.core.StaticApplication
import com.mle.util.{Log, FileUtilities, Util}
import java.nio.file.{Files, Path, Paths}
import scala.Some

/**
 * Starts Play Framework 2, does not create a RUNNING_PID file.
 *
 * An alternative to the official ways to start Play,
 * this integrates better with my more generic init scripts.
 *
 * @author mle
 */
trait PlayLifeCycle extends Log {

  var nettyServer: Option[NettyServer] = None

  def appName: String

  def main(args: Array[String]) {
    // TODO add option to kill running instance gracefully using e.g. remote akka actors
    start()
  }

  def start() {
    log info s"Starting $appName..."
    FileUtilities.basePath = Paths get sys.props.get(s"$appName.home").getOrElse(sys.props("user.dir"))
    addConfFileToSysProps(appName)

    /**
     * NettyServer.createServer insists on writing a RUNNING_PID file.
     * Fuck that.
     */
    nettyServer = Some(createServer())
  }

  private def createServer() = {
    val server = new NettyServer(
      new StaticApplication(FileUtilities.basePath.toFile),
      Option(System.getProperty("http.port")).map(Integer.parseInt).orElse(Some(9000)),
      Option(System.getProperty("https.port")).map(Integer.parseInt),
      Option(System.getProperty("http.address")).getOrElse("0.0.0.0")
    )
    Util.addShutdownHook(server.stop())
    server
  }

  /**
   * If a file named app_name_here.conf exists, reads it and adds any
   * properties in it to the system properties.
   *
   * @param confNameWithoutExtension name of conf, typically the name of the app
   */
  def addConfFileToSysProps(confNameWithoutExtension: String) {
    // adds settings in app conf to system properties
    val confFile = FileUtilities.pathTo(s"$confNameWithoutExtension.conf")
    if (Files.exists(confFile)) {
      val props = propsFromFile(confFile)
      // makes keystore file path absolute
      val keystorePathKey = "https.keyStore"
      val sysPropsAdditions = props.get(keystorePathKey).map(keyStorePath => {
        props.updated(keystorePathKey, FileUtilities.pathTo(keyStorePath).toAbsolutePath.toString)
      }).getOrElse(props)
      sys.props ++= sysPropsAdditions
    }
  }

  private def propsFromFile(file: Path): Map[String, String] = {
    if (Files.exists(file)) {
      Util.resource(io.Source.fromFile(file.toFile))(src => {
        val kvs = src.getLines().flatMap(line => {
          val kv = line.split("=", 2)
          if (kv.size >= 2) {
            Some(kv(0) -> kv(1))
          } else {
            None
          }
        })
        Map(kvs.toList: _*)
      })
    } else {
      Map.empty[String, String]
    }
  }
}
