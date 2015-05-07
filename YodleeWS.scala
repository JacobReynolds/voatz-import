package org.voatz.yodlee

import scala.language.implicitConversions
import scala.language.postfixOps
import language.experimental.macros

import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws._
import play.api.libs.ws.ning._
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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

sealed trait AuthenticateCobrandResponseType
case object InvalidCobrandCredentialsException extends AuthenticateCobrandResponseType
case object CobrandUserAccountLockedException extends AuthenticateCobrandResponseType

case class AuthenticateCobrandResponse(
  cobrandId: Int,
  channelId: Int,
  locale: String,
  tncVersion: Int,
  applicationId: String,
  cobrandConversationCredentials: Credentials,
  preferenceInfo: PreferenceInfo
) extends AuthenticateCobrandResponseType
object AuthenticateCobrandResponse {
  implicit val reads = Json.reads[AuthenticateCobrandResponse]
}

case object YodleeWS extends App {
  /* client code */
  val voatzLogin = "sbCobvulcan"
  val voatzPass = "e50e4dfa-407e-4713-9008-640be6f78fea"
  authenticateCobrand(voatzLogin, voatzPass)

  /* NB need to initialize WSClient because we're not starting a Play app
   * http://carminedimascio.com/2015/02/how-to-use-the-play-ws-library-in-a-standalone-scala-app/
   */
  val client: WSClient = {
    /* NB DefaultClientConfig is called NingClientConfig in Play 2.4 */
    val config = new NingAsyncHttpClientConfigBuilder(NingWSClientConfig()).build
    val builder = new AsyncHttpClientConfig.Builder(config)
    new NingWSClient(builder.build)
  }
  val yodleeRestURL = "https://rest.developer.yodlee.com/services/srest/restserver/v1.0"

  def authenticateCobrand(cobrandLogin: String, cobrandPassword: String): Unit = {
    val url = yodleeRestURL + "/authenticate/coblogin"

    val request = client.url(url).
      withHeaders("Content-Type" -> "application/x-www-form-urlencoded; charset=\"utf-8\"",
                  "Connection" -> "close")

    val data = Map("cobrandLogin" -> Seq(cobrandLogin),
                   "cobrandPassword" -> Seq(cobrandPassword))

    val response = request.post(data) withTimeout timeoutEx("request timeout")

    val result = response map { _.json.validate[AuthenticateCobrandResponse] }

    result foreach { r => {
      println(r)
      client.close()
      shutdown()
    }}

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
}
