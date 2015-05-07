package org.voatz.yodlee

import scala.language.implicitConversions
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async.{async, await}
import akka.actor.ActorSystem

import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws._
import play.api.libs.ws.ning._
import play.api.libs.json._
import org.slf4j.LoggerFactory

import org.voatz.AsyncUtils._

case class CurrencyNotationType(currencyNotationType: String)
object CurrencyNotationType {
  implicit val reads = Json.reads[CurrencyNotationType]
}

case class NumberFormat(
  decimalSeparator: String,
  groupingSeparator: String,
  groupPattern: String)
object NumberFormat {
  implicit val reads = Json.reads[NumberFormat]
}

case class Credentials(sessionToken: String)
object Credentials {
  implicit val reads = Json.reads[Credentials]
}

case class PreferenceInfo(
  currencyCode: String,
  timeZone: String,
  dateFormat: String,
  currencyNotationType: CurrencyNotationType,
  numberFormat: NumberFormat
)
object PreferenceInfo {
  implicit val reads = Json.reads[PreferenceInfo]
}

sealed trait YodleeAuthenticate
case object InvalidCobrandCredentialsException extends YodleeAuthenticate
case object CobrandUserAccountLockedException extends YodleeAuthenticate

case class AuthenticateCobrandResponse(
  cobrandId: Int,
  channelId: Int,
  locale: String,
  tncVersion: Int,
  applicationId: String,
  cobrandConversationCredentials: Credentials,
  preferenceInfo: PreferenceInfo
) extends YodleeAuthenticate
object AuthenticateCobrandResponse {
  implicit val reads = Json.reads[AuthenticateCobrandResponse]
}

case object YodleeWS extends App {
  /* System for Timeout */
  implicit val system = ActorSystem("timeoutSystem")

  /* Logger */
  val log = LoggerFactory.getLogger("org.voatz.yodlee.YodleeWS")

  /* URL and Headers */
  val yodleeURL = "https://rest.developer.yodlee.com/services/srest/restserver/v1.0"
  val yodleeHdr1 = "Content-Type" -> "application/x-www-form-urlencoded; charset=\"utf-8\""
  val yodleeHhd2 = "Connection" -> "close"

  /* NB need to initialize a custom WSClient because we're not starting a Play app
   * http://carminedimascio.com/2015/02/how-to-use-the-play-ws-library-in-a-standalone-scala-app/
   */
  val client: WSClient = {
    /* NB DefaultClientConfig is called NingClientConfig in Play 2.4 */
    val config = new NingAsyncHttpClientConfigBuilder(NingWSClientConfig()).build
    val builder = new AsyncHttpClientConfig.Builder(config)
    new NingWSClient(builder.build)
  }

  /* Step 1 of the API */
  def authenticateCobrand(cobrandLogin: String, cobrandPassword: String):
    Future[YodleeAuthenticate] = {

    val url = yodleeURL + "/authenticate/coblogin"
    val request = client.url(url).withHeaders(yodleeHdr1, yodleeHhd2)
    val data = Map("cobrandLogin" -> Seq(cobrandLogin),
                   "cobrandPassword" -> Seq(cobrandPassword))

    def validateResponse(json: JsValue): YodleeAuthenticate = {
      json.validate[AuthenticateCobrandResponse].fold( valid = identity,
        invalid = _ => (json \\ "errorDetail") map (_.asOpt[String]) head match {
          case Some("Invalid Cobrand Credentials") => InvalidCobrandCredentialsException
          case Some("Cobrand User Account Locked") => CobrandUserAccountLockedException
          case None | Some(_) => ??? // TODO should never happen
        })
    }

    async {
      implicit val timeout: FiniteDuration = 900 millis
      val resp = await { request.post(data) withTimeout timeoutEx("request timeout") }
      validateResponse(resp.json)
    }
  }

  def registerNewConsumer = ???
  def loginConsumer = ???
  def getContentServiceInfo = ???
  def displayLoginForm = ???
  def addItemAndStartVerification = ???
  def getMFAResponse = ???
  def getIAVInfo = ???

  def shutdown(): Unit = {
    system.shutdown
  }

  /* Client Code */
  val voatzLogin = "sbCobvulcan"
  val voatzPass = "e50e4dfa-407e-4713-9008-640be6f78fea"

  val authRes = authenticateCobrand(voatzLogin, voatzPass)

  authRes foreach { r => {
      log.info(r toString)
      client.close()
      shutdown()
    }
  }

  for (exc <- authRes.failed) {
    log.info(exc.getMessage)
    client.close()
    /* after shutdown is called once, cannot reinitialize the system in Utils ->
     * object, not class */
    shutdown()
  }
}
