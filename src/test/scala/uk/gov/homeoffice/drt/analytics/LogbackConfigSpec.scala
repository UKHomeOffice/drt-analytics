package uk.gov.homeoffice.drt.analytics

import ch.qos.logback.classic.{ AsyncAppender, Level, LoggerContext }
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.{ ILoggingEvent, LoggingEvent }
import ch.qos.logback.classic.util.LogbackMDCAdapter
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.status.Status
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.Logger

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID
import scala.jdk.CollectionConverters._
import scala.util.Using

class LogbackConfigSpec extends AnyWordSpec with Matchers {
  "The Logback configuration" should {
    "use JSON when NO_JSON_LOGGING is not defined" in {
      val output = renderLog(None)
      val json = new ObjectMapper().readTree(output)

      json.get("level").asText() shouldBe "INFO"
      json.get("message").asText() shouldBe "logging configuration probe"
      json.has("thread") shouldBe false
    }

    "use plain text when NO_JSON_LOGGING is defined, including an empty value" in {
      Seq("", "1", "false").foreach { value =>
        val output = renderLog(Some(value))

        output should include("[INFO]")
        output should include(" - logging configuration probe")
        output.trim should not startWith "{"
      }
    }
  }

  private def renderLog(noJsonLogging: Option[String]): String = {
    val resource = Option(getClass.getClassLoader.getResourceAsStream("logback.xml"))
      .getOrElse(fail("logback.xml not found"))
    val xml = Using.resource(resource)(stream => new String(stream.readAllBytes(), UTF_8))
    val condition = """isDefined("NO_JSON_LOGGING")"""
    xml should include(condition)

    val testKey = s"NO_JSON_LOGGING_TEST_${UUID.randomUUID().toString.replace("-", "")}"
    val testXml = xml.replace(condition, s"""isDefined("$testKey")""")
    val context = new LoggerContext()
    context.setMDCAdapter(new LogbackMDCAdapter())
    noJsonLogging.foreach(context.putProperty(testKey, _))

    try {
      val configurator = new JoranConfigurator()
      configurator.setContext(context)
      Using.resource(new ByteArrayInputStream(testXml.getBytes(UTF_8)))(configurator.doConfigure)

      val problems = context.getStatusManager.getCopyOfStatusList.asScala.filter(_.getLevel >= Status.WARN)
      withClue(problems.mkString("\n")) {
        problems shouldBe empty
      }

      val logger = context.getLogger(Logger.ROOT_LOGGER_NAME)
      val async = Option(logger.getAppender("ASYNC")) match {
        case Some(appender: AsyncAppender) => appender
        case other                         => fail(s"Expected an async appender, got $other")
      }
      val console = Option(async.getAppender("STDOUT")) match {
        case Some(appender: ConsoleAppender[ILoggingEvent]) => appender
        case other                                          => fail(s"Expected a console appender, got $other")
      }

      val event = new LoggingEvent()
      event.setLoggerName(logger.getName)
      event.setLoggerContext(context)
      event.setLoggerContextRemoteView(context.getLoggerContextRemoteView)
      event.setLevel(Level.INFO)
      event.setMessage("logging configuration probe")
      event.setTimeStamp(System.currentTimeMillis())
      event.setThreadName("test")
      event.setMDCPropertyMap(Map.empty[String, String].asJava)
      new String(console.getEncoder.encode(event), UTF_8)
    } finally context.stop()
  }
}
