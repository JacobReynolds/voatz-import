package org.nimsim.voatz.yodlee

import scala.collection.immutable.ListMap
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
import play.api.data.validation._
import org.slf4j.LoggerFactory

import org.nimsim.voatz.AsyncUtils._

/* YodleeWS singleton object */
object YodleeWS {
  /* System for Timeout */
  implicit val system = ActorSystem("timeoutSystem")

  /* Logger */
  val log = LoggerFactory.getLogger("org.voatz.yodlee.YodleeWS")

  /* NB need to initialize a custom WSClient because we're not starting a Play app
   * http://carminedimascio.com/2015/02/how-to-use-the-play-ws-library-in-a-standalone-scala-app/
   */
  val client: WSClient = {
    /* NB DefaultClientConfig is called NingClientConfig in Play 2.4 */
    val config = new NingAsyncHttpClientConfigBuilder(NingWSClientConfig()).build
    val builder = new AsyncHttpClientConfig.Builder(config)
    new NingWSClient(builder.build)
  }

  def authCobrand(in: AuthenticateInput): Future[Either[YodleeException, YodleeAuthenticate]] = {
    val data = Map("cobrandLogin" -> Seq(in.cobrandLogin),
                   "cobrandPassword" -> Seq(in.cobrandPassword))

    implicit val authenticateSubUrl = "/authenticate/coblogin"
    def validateResponse(json: JsValue): Either[YodleeException, YodleeAuthenticate] = {
      json.validate[AuthenticateCobrand] fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    async {
      val resp = await { sendRequest(data, "cobrand authenticate request timeout") }
      validateResponse(resp json)
    }
  }

  def loginConsumer(in: LoginInput): Future[Either[YodleeException, YodleeLogin]] = {
    val data = Map("cobSessionToken" -> Seq(in.cobSessionToken),
                   "login" -> Seq(in.login),
                   "password" -> Seq(in.password))

    implicit val loginSubUrl = "/authenticate/login"
    def validateResponse(json: JsValue): Either[YodleeException, YodleeLogin] = {
      json.validate[UserInfo] fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    async {
      val resp = await { sendRequest(data, "login request timeout") }
      validateResponse(resp json)
    }
  }

  def registerNewConsumer(in: RegisterInput): Future[Either[YodleeException, YodleeRegister]] = {
    implicit val registerSubUrl = "/jsonsdk/UserRegistration/register3"
    val request = mkWSRequest(registerSubUrl)
    val data = {
      val cred = Map(
       "cobSessionToken" -> Seq(in.cobSessionToken),
       "userCredentials.loginName" -> Seq(in.userCredentials.loginName),
       "userCredentials.password" -> Seq(in.userCredentials.password),
       "userCredentials.objectInstanceType" -> Seq(in.userCredentials.objectInstanceType)
      )

      val profile = {
        val p = in.userProfile
        val email = Map("userProfile.emailAddress" -> Seq(p.emailAddress))
        val firstName = p.firstName map {
            x => Map("userProfile.firstName" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val middleInitial = p.middleInitial map {
            x => Map("userProfile.middleInitial" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val lastName = p.lastName map {
            x => Map("userProfile.lastName" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val objectType = p.objectInstanceType map {
            x => Map("userProfile.objectInstanceType" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val address1 = p.address1 map {
            x => Map("userProfile.address1" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val address2 = p.address2 map {
            x => Map("userProfile.address2" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val city = p.city map {
            x => Map("userProfile.city" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val country = p.country map {
            x => Map("userProfile.country" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val prefs = {
          val pxs = in.userPreferences
          (for (i <- 0 until pxs.size; t = pxs(i)) yield {
            val (k,v) = t
            s"userPreferences[$i]" -> Seq(k,v)
          }) toMap
        }

        email ++ prefs ++ firstName ++ middleInitial ++ lastName ++ objectType ++
          address1 ++ address2 ++ city ++ country
      }

      cred ++ profile
    }

    def validateResponse(json: JsValue): Either[YodleeException, YodleeRegister] = {
      json.validate[UserRegisterInfo] fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    async {
      val resp = await { sendRequest(data, "register request timeout") }
      validateResponse(resp json)
    }
  }

  def searchSiteWithFilter(in: SearchSiteInput): Future[Either[YodleeException,
  YodleeSiteInfo]] = {
    val data = Map(
      "cobSessionToken" -> Seq(in.cobSessionToken),
      "userSessionToken" -> Seq(in.userSessionToken),
      "siteSearchString" -> Seq(in.siteSearchString),
      "siteSearchFilter.retrieveIavSitesOnly" ->
        Seq(in.siteSearchFilter.retrieveIavSitesOnly toString),
      "siteSearchFilter.containers" -> Seq(in.siteSearchFilter.containers)
    )

    implicit val searchSiteSubUrl = "/jsonsdk/SiteTraversal/searchSiteWithFilter"

    def validateResponse(json: JsValue): Either[YodleeException, YodleeSiteInfo] = {
      json.validate[YodleeSiteInfo] fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    async {
      val resp = await { sendRequest(data, "searchSite request timeout") }
      validateResponse(resp json)
    }
  }

  def getContentServiceInfo(in: ContentServiceInfoInput):
    Future[Either[YodleeException, YodleeServiceInfo]] = {
    val data = Map("cobSessionToken" -> Seq(in.cobSessionToken),
                   "routingNumber" -> Seq(in.routingNumber toString),
                   "notrim" -> Seq(in.notrim toString))

    implicit val serviceInfoSubUrl = "/jsonsdk/RoutingNumberService/getContentServiceInfoByRoutingNumber"
    def validateResponse(json: JsValue): Either[YodleeException, YodleeServiceInfo] = {
      val seq1 = json.validate[ContentServiceInfoSeq1] fold(
        invalid = _ => invalidHandler(json), identity
      )
      val seq2 = json.validate[ContentServiceInfoSeq2] fold(
        invalid = _ => invalidHandler(json), identity
      )
      val seq3 = json.validate[ContentServiceInfoSeq3] fold(
        invalid = _ => invalidHandler(json), identity
      )
      val seq4 = json.validate[ContentServiceInfoSeq4] fold(
        invalid = _ => invalidHandler(json), identity
      )

      (seq1, seq2, seq3, seq4) match {
        case (ex: YodleeException, _, _, _) => Left(ex)
        case (_, ex: YodleeException, _, _) => Left(ex)
        case (_, _, ex: YodleeException, _) => Left(ex)
        case (_, _, _, ex: YodleeException) => Left(ex)
        case _ => Right(ContentServiceInfo(seq1.asInstanceOf[ContentServiceInfoSeq1],
                                           seq2.asInstanceOf[ContentServiceInfoSeq2],
                                           seq3.asInstanceOf[ContentServiceInfoSeq3],
                                           seq4.asInstanceOf[ContentServiceInfoSeq4]))
      }
    }

    async {
      val resp = await { sendRequest(data, "serviceInfo request timeout") }
      log info(Json prettyPrint(resp json))
      validateResponse(resp json)
    }
  }

  /* no longer used in the new IAV API */
  def getLoginForm(in: LoginFormInput): Future[Either[YodleeException, YodleeLoginForm]] = {
    val data = Map("cobSessionToken" -> Seq(in.cobSessionToken),
                   "contentServiceId" -> Seq(in.contentServiceId toString))

    implicit val loginFormSubUrl = "/jsonsdk/ItemManagement/getLoginFormForContentService"
    def validateResponse(json: JsValue): Either[YodleeException, YodleeLoginForm] = {
      json.validate[LoginForm] fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    async {
      val resp = await { sendRequest(data, "loginForm request timeout") }
      validateResponse(resp json)
    }
  }

  def startVerification(in: StartVerificationInput): Future[Either[YodleeException, YodleeIAVRefreshStatus]] = {
    implicit val verificationSubUrl =
      "/jsonsdk/ExtendedInstantVerificationDataService/addItemAndStartVerificationDataRequest1"

    val data: Map[String, Seq[String]] = {
      val tokens = Map(
        "cobSessionToken" -> Seq(in.cobSessionToken),
        "userSessionToken" -> Seq(in.userSessionToken),
        "credentialFields.enclosedType" -> Seq(in.enclosedType)
      )

      val cfms = {
        val cfs = in.credentialFields
        val xs = for (i <- 0 until cfs.size; cf = cfs(i) ) yield Map (
          s"credentialFields[$i].displayName" -> Seq(cf.displayName),
          s"credentialFields[$i].fieldType.typeName" -> Seq(cf.fieldType.typeName),
          s"credentialFields[$i].helpText" -> Seq(cf.helpText toString),
          s"credentialFields[$i].isEditable" -> Seq(cf.isEditable toString),
          s"credentialFields[$i].maxlength" -> Seq(cf.maxlength toString),
          s"credentialFields[$i].name" -> Seq(cf.name),
          s"credentialFields[$i].size" -> Seq(cf.size toString),
          s"credentialFields[$i].value" -> Seq(cf.value),
          s"credentialFields[$i].valueIdentifier" -> Seq(cf.valueIdentifier),
          s"credentialFields[$i].valueMask" -> Seq(cf.valueMask)
        )
        (Map.empty[String, Seq[String]] /: xs) (_ ++ _)
      }

      val iavRequest = {
        val serviceId =
          Map("iavRequest.contentServiceId" -> Seq(in.iavRequest.contentServiceId toString))
        val loginResponse = in.iavRequest.isLoginResponseRequired map { x =>
            Map("iavRequest.isLoginResponseRequired" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val accountSummary = in.iavRequest.isAccountSummaryResponseRequired map { x =>
            Map("iavRequest.isAccountSummaryResponseRequired" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val transactionResponse = in.iavRequest.isTransactionResponseRequired map { x =>
            Map("iavRequest.isTransactionResponseRequired" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]
        val transactionLimit = in.iavRequest.transactionLimit map { x =>
            Map("iavRequest.transactionLimit" -> Seq(x toString))
          } getOrElse Map.empty[String, Seq[String]]

        serviceId ++ loginResponse ++ accountSummary ++ transactionResponse ++
          transactionLimit
      }
      tokens ++ cfms ++ iavRequest
    }

    def validateResponse(json: JsValue): Either[YodleeException, YodleeIAVRefreshStatus] = {
      json.validate[IAVRefreshStatus] fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    async {
      val resp = await { sendRequest(data, "startVerification request timeout") }
      validateResponse(resp json)
    }
  }

  def getMFAResponse(in: GetMFAInput): Future[Either[YodleeException, YodleeMFA]] = {
    val data = Map("cobSessionToken" -> Seq(in.cobSessionToken),
                   "userSessionToken" -> Seq(in.userSessionToken),
                   "itemId" -> Seq(in.itemId toString))

    implicit val getMFASubUrl = "/jsonsdk/Refresh/getMFAResponse"
    def validateResponse(json: JsValue): Either[YodleeException, YodleeMFA] = {
      json.validate[YodleeMFA] fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    async {
      val resp = await { sendRequest(data, "getMFA request timeout", 90000) }
      log.info(Json prettyPrint(resp json))
      validateResponse(resp json)
    }
  }

  def putMFARequest(in: PutMFAInput): Future[Either[YodleeException, YodleeMFA]] = {
    val data: Map[String, Seq[String]] = {
      val tokens = Map(
        "cobSessionToken" -> Seq(in.getInput.cobSessionToken),
        "userSessionToken" -> Seq(in.getInput.userSessionToken),
        "itemId" -> Seq(in.getInput.itemId toString)
      )

      val respMap = in.userResponse match {
        case MFATokenResponse(o, t) => Map("userResponse.objectInstanceType" -> Seq(o),
                                           "userResponse.token" -> Seq(t))
        case MFAImageResponse(o, str) => Map("userResponse.objectInstanceType" -> Seq(o),
                                             "userResponse.imageString" -> Seq(str))
        case MFAQAResponse(o, vs) => {
          val xs = for (i <- 0 until vs.size; r = vs(i) ) yield Map(
            s"quesAnsDetailArray[$i].answer" -> Seq(r.answer),
            s"quesAnsDetailArray[$i].answerFieldType" -> Seq(r.answerFieldType),
            s"quesAnsDetailArray[$i].metaData" -> Seq(r.metaData),
            s"quesAnsDetailArray[$i].question" -> Seq(r.question),
            s"quesAnsDetailArray[$i].questionFieldType" -> Seq(r.questionFieldType)
          )
          Map("userResponse.objectInstanceType" -> Seq(o)) ++
            (Map.empty[String, Seq[String]] /: xs) {_ ++ _}
        }
      }
      tokens ++ respMap
    }

    implicit val putMFASubUrl = "/jsonsdk/Refresh/putMFARequest"
    def validateResponse(json: JsValue): Either[YodleeException, YodleeMFA] = {
      json.validate[MFAPutResponse] fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    async {
      val resp = await { sendRequest(data, "putMFA request timeout") }
      validateResponse(resp json)
    }
  }

  def getVerificationData(in: VerificationDataInput):
  Future[Either[YodleeException, List[YodleeVerificationData]]] = {
    val data = {
      val tokens = Map("cobSessionToken" -> Seq(in.cobSessionToken),
                       "userSessionToken" -> Seq(in.userSessionToken))
      val ixs = {
        val xs = for (i <- 0 until in.itemIds.size; item = in.itemIds(i) )
                  yield Map(s"itemIds[$i]" -> Seq(item toString))
        (Map.empty[String, Seq[String]] /: xs) {_ ++ _}
      }
      tokens ++ ixs
    }

    implicit val getDataSubUrl = "/jsonsdk/InstantVerificationDataService/getItemVerificationData"
    def validateResponse(json: JsValue): Either[YodleeException, List[YodleeVerificationData]] = {
      json.validate[List[YodleeVerificationData]] fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    async {
      val resp = await { sendRequest(data, "getVerificationData request timeout") }
      log info(Json prettyPrint(resp json))
      validateResponse(resp json)
    }
  }

  def shutdown(by: String): Unit = {
    log.info(s"shutdown by $by")
    client close()
    system shutdown
  }

  private def mkWSRequest(subUrl: String): WSRequest = {
    val yodleeURL = "https://rest.developer.yodlee.com/services/srest/restserver/v1.0"
    val yodleeHdr1 = "Content-Type" -> "application/x-www-form-urlencoded; charset=\"utf-8\""
    val yodleeHhd2 = "Connection" -> "close"

    client.url(yodleeURL + subUrl).withHeaders(yodleeHdr1, yodleeHhd2)
  }

  private def invalidHandler(json: JsValue)(implicit from: String): YodleeException = {
    json.validate[YodleeException] fold(
      valid = identity,
      invalid = _ => YodleeSimpleException(s"$from: ${Json.prettyPrint(json)}")
    )
  }

  private def sendRequest(data: Map[String, Seq[String]],
                          eMsg: String,
                          time: Int = 3000)(implicit subUrl: String): Future[WSResponse] = {
    implicit val timeout: FiniteDuration = time millis
    val resp = mkWSRequest(subUrl).post(data) withTimeout timeoutEx(eMsg)
    resp
  }

}
