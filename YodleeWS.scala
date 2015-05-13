package org.nimsim.voatz.yodlee

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

/* ========================= */
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

  def mkWSRequest(subUrl: String): WSRequest = {
    val yodleeURL = "https://rest.developer.yodlee.com/services/srest/restserver/v1.0"
    val yodleeHdr1 = "Content-Type" -> "application/x-www-form-urlencoded; charset=\"utf-8\""
    val yodleeHhd2 = "Connection" -> "close"

    client.url(yodleeURL + subUrl).withHeaders(yodleeHdr1, yodleeHhd2)
  }

  def shutdown(by: String): Unit = {
    log.info(s"shutdown by $by")
    client.close()
    system.shutdown
  }

  /* Step 1 of the API */
  def authCobrand(in: AuthenticateInput): Future[Either[YodleeException, YodleeAuthenticate]] = {
    val request = mkWSRequest("/authenticate/coblogin")
    val data = Map("cobrandLogin" -> Seq(in.cobrandLogin),
                   "cobrandPassword" -> Seq(in.cobrandPassword))

    def validateResponse(json: JsValue): Either[YodleeException, YodleeAuthenticate] = {
      json.validate[AuthenticateCobrand].fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    def invalidHandler(json: JsValue): YodleeException = {
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        case Some(msg) => YodleeException("", "", "", msg)
        case None => YodleeException("", "", "", "error not specified")
      }
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("authenticate request timeout")
      }
      validateResponse(resp.json)
    }
  }

  def loginConsumer(in: LoginInput): Future[Either[YodleeException, YodleeLogin]] = {
    val request = mkWSRequest("/authenticate/login")
    val data = Map("cobSessionToken" -> Seq(in.cobSessionToken),
                   "login" -> Seq(in.login),
                   "password" -> Seq(in.password))

    def validateResponse(json: JsValue): Either[YodleeException, YodleeLogin] = {
      json.validate[UserInfo].fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    def invalidHandler(json: JsValue) : YodleeException = {
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        case Some(msg) => YodleeException("", "", "", msg)
        case None => YodleeException("", "", "", "error not specified")
      }
    }

    async {
      implicit val timeout: FiniteDuration = 3000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("login request timeout")
      }
      validateResponse(resp.json)
    }
  }

  def registerNewConsumer(in: RegisterInput): Future[Either[YodleeException, YodleeRegister]] = {
    val request = mkWSRequest("/jsonsdk/UserRegistration/register3")
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
      json.validate[UserRegisterInfo].fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    def invalidHandler(json: JsValue): YodleeException = {
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        case Some(msg) => YodleeException("", "", "", msg)
        case None => YodleeException("", "", "", "error not specified")
      }
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("getVerificationData request timeout")
      }
      validateResponse(resp.json)
    }
  }

  def getContentServiceInfo(in: ContentServiceInfoInput): Future[Either[YodleeException, YodleeServiceInfo]] = {
    val request = mkWSRequest("/jsonsdk/RoutingNumberService/getContentServiceInfoByRoutingNumber")

    val data = Map("cobSessionToken" -> Seq(in.cobSessionToken),
                   "routingNumber" -> Seq(in.routingNumber toString),
                   "notrim" -> Seq(in.notrim toString))

    def invalidHandler(json: JsValue): YodleeException = {
      json.validate[YodleeException].fold(
        valid = identity,
        invalid = _ => YodleeException("", "", "", "error not specified")
      )
    }

    def validateResponse(json: JsValue): Either[YodleeException, YodleeServiceInfo] = {
      val seq1 = json.validate[ContentServiceInfoSeq1].fold(
        invalid = _ => invalidHandler(json), identity
      )
      val seq2 = json.validate[ContentServiceInfoSeq2].fold(
        invalid = _ => invalidHandler(json), identity
      )
      val seq3 = json.validate[ContentServiceInfoSeq3].fold(
        invalid = _ => invalidHandler(json), identity
      )

      (seq1, seq2, seq3) match {
        case (ex: YodleeException, _, _) => Left(ex)
        case (_, ex: YodleeException, _) => Left(ex)
        case (_, _, ex: YodleeException) => Left(ex)
        case _ => Right(ContentServiceInfo(seq1.asInstanceOf[ContentServiceInfoSeq1],
                                           seq2.asInstanceOf[ContentServiceInfoSeq2],
                                           seq3.asInstanceOf[ContentServiceInfoSeq3]))
      }
    }

    async {
      implicit val timeout: FiniteDuration = 3000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("serviceInfo request timeout")
      }
      validateResponse(resp.json)
    }
  }

  def getLoginForm(in: LoginFormInput): Future[Either[YodleeException, YodleeLoginForm]] = {
    val request = mkWSRequest("/jsonsdk/ItemManagement/getLoginFormForContentService")
    val data = Map("cobSessionToken" -> Seq(in.cobSessionToken),
                   "contentServiceId" -> Seq(in.contentServiceId toString))

    def validateResponse(json: JsValue): Either[YodleeException, YodleeLoginForm] = {
      json.validate[LoginForm].fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    def invalidHandler(json: JsValue): YodleeException = {
      json.validate[YodleeException].fold(
        valid = identity,
        invalid = _ => YodleeException("", "", "", "error not specified")
      )
    /*
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        //case Some("Invalid Cobrand Conversation Credentials") =>
          //InvalidCobrandConversationCredentialsException
        //case Some("Content Service Not Found") => ContentServiceNotFoundException
        case Some(msg) => YodleeException("", "", "", msg)
        case None => YodleeException("", "", "", "error not specified")
      }
      */
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("loginForm request timeout")
      }
      validateResponse(resp.json)
    }
  }

  def startVerification(in: StartVerificationInput): Future[Either[YodleeException, YodleeIAVRefreshStatus]] = {
    val request = mkWSRequest("/jsonsdk/ExtendedInstantVerificationDataService/addItemAndStartVerificationDataRequest")

    val data: Map[String, Seq[String]] = {
      val tokens = Map(
        "cobSessionToken" -> Seq(in.cobSessionToken),
        "userSessionToken" -> Seq(in.userSessionToken),
        "contentServiceId" -> Seq(in.contentServiceId toString)
      )

      val accNum = {
        val opt = in.accountNumber map { x => Map("accountNumber" -> Seq(x toString)) }
        opt getOrElse Map.empty[String, Seq[String]]
      }

      val routNum = {
        val opt = in.accountNumber map { x => Map("routingNumber" -> Seq(x toString)) }
        opt getOrElse Map.empty[String, Seq[String]]
      }

      val encType = Map("credentialFields.enclosedType" -> Seq(in.enclosedType))

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
        (Map.empty[String, Seq[String]] /: xs) {_ ++ _}
      }

      tokens ++ accNum ++ routNum ++ encType ++ cfms
    }

    def validateResponse(json: JsValue): Either[YodleeException, YodleeIAVRefreshStatus] = {
      log.info(json toString)
      json.validate[IAVRefreshStatus].fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    def invalidHandler(json: JsValue): YodleeException = {
      json.validate[YodleeException].fold(
        valid = identity,
        invalid = _ => YodleeException("", "", "", "error not specified")
      )
    /*
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
          case Some("Invalid Cobrand Conversation Credentials") =>
            InvalidCobrandConversationCredentialsException
          case Some("Invalid Conversation Credentials") =>
            InvalidConversationCredentialsException
          case Some("Content Service Not Found") => ContentServiceNotFoundException
          case Some("Illegal Argument Value") => IllegalArgumentValueException
          case Some("Incomplete Argument") => IncompleteArgumentException
          case Some("IAV Data Request Not Supported") => IAVDataRequestNotSupportedException
          case Some(msg) => YodleeException(msg)
          case None => YodleeException("error not specified")
      }
      */
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("start verification request timeout")
      }
      validateResponse(resp.json)
    }
  }

  def getMFAResponse(in: GetMFAInput): Future[Either[YodleeException, YodleeMFA]] = {
    val request = mkWSRequest("/jsonsdk/Refresh/getMFAResponse")
    val data = Map("cobSessionToken" -> Seq(in.cobSessionToken),
                   "userSessionToken" -> Seq(in.userSessionToken),
                   "itemId" -> Seq(in.itemId toString))

    def invalidHandler(json: JsValue): YodleeException = {
      json.validate[YodleeException].fold(
        valid = identity,
        invalid = _ => YodleeException("", "", "", "error not specified")
      )
        /*
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        case Some("InvalidCobrandConversationCredentialsException") =>
          InvalidCobrandConversationCredentialsException
        case Some("InvalidConversationCredentialsException") =>
          InvalidConversationCredentialsException
        case Some("InvalidItemException") => InvalidItemException
        case Some("MFARefreshException") => MFARefreshException
        case Some(msg) => YodleeException(msg)
        case None => YodleeException("", "", "", "None")
      }
        */
    }

    def validateResponse(json: JsValue): Either[YodleeException, YodleeMFA] = {
      val token = json.validate[MFARefreshInfoToken].fold(
        invalid = _ => invalidHandler(json), identity
      )
      val image = json.validate[MFARefreshInfoImage].fold(
        invalid = _ => invalidHandler(json), identity
      )
      val question = json.validate[MFARefreshInfoQuestion].fold(
        invalid = _ =>invalidHandler(json), identity
      )

      (token, image, question) match {  // TODO: extremely klunky => fix
        case (v: YodleeMFA, _: YodleeException, _: YodleeException) => Right(v)
        case (_: YodleeException, v: YodleeMFA, _: YodleeException) => Right(v)
        case (_: YodleeException, _: YodleeException, v: YodleeMFA) => Right(v)
        case (e: YodleeException,
              YodleeException("", "", "", "None"),
              YodleeException("", "", "", "None")) => Left(e)
        case (YodleeException("", "", "", "None"),
              e: YodleeException,
              YodleeException("", "", "", "None")) => Left(e)
        case (YodleeException("", "", "", "None"),
              YodleeException("", "", "", "None"),
              e: YodleeException) => Left(e)
        case _ => Left(YodleeException("", "", "", "error not specified"))
      }
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("getMFA request timeout")
      }
      validateResponse(resp.json)
    }

  }

  def putMFAResponse(in: PutMFAInput): Future[Either[YodleeException, YodleeMFAPutResponse]] = {
    val request = mkWSRequest("/jsonsdk/Refresh/putMFAResponse")
    val data: Map[String, Seq[String]] = {
      val tokens = Map(
        "cobSessionToken" -> Seq(in.getInput.cobSessionToken),
        "userSessionToken" -> Seq(in.getInput.userSessionToken),
        "itemId" -> Seq(in.getInput.itemId toString)
      )

      val respMap = in.userResponse match {
        case MFATokenResponse(t, v) => Map("userResponse.objectInstanceType" -> Seq(t),
                                           "userResponse.token" -> Seq(v))
        case MFAImageResponse(t, v) => Map("userResponse.objectInstanceType" -> Seq(t),
                                           "userResponse.imageString" -> Seq(v))
        case MFAQAResponse(t, vs) => {
          val xs = for (i <- 0 until vs.size; r = vs(i) ) yield Map(
            s"quesAnsDetailArray[$i].answer" -> Seq(r.answer),
            s"quesAnsDetailArray[$i].answerFieldType" -> Seq(r.answerFieldType),
            s"quesAnsDetailArray[$i].metaData" -> Seq(r.metaData),
            s"quesAnsDetailArray[$i].question" -> Seq(r.question),
            s"quesAnsDetailArray[$i].questionFieldType" -> Seq(r.questionFieldType)
          )
          Map("userResponse.objectInstanceType" -> Seq(t)) ++
            (Map.empty[String, Seq[String]] /: xs) {_ ++ _}
        }
      }
      tokens ++ respMap
    }

    def validateResponse(json: JsValue): Either[YodleeException, YodleeMFAPutResponse] = {
      json.validate[YodleeMFAPutResponse].fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    def invalidHandler(json: JsValue): YodleeException = {
      json.validate[YodleeException].fold(
        valid = identity,
        invalid = _ => YodleeException("", "", "", "error not specified")
      )
        /*
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        case Some("InvalidCobrandConversationCredentialsException") =>
          InvalidCobrandConversationCredentialsException
        case Some("InvalidConversationCredentialsException") =>
          InvalidConversationCredentialsException
        case Some("InvalidItemException") => InvalidItemException
        case Some("MFARefreshException") => MFARefreshException
        case Some("IllegalArgumentValueException") => IllegalArgumentValueException
        case Some(msg) => YodleeException(msg)
        case None => YodleeException("", "", "", "None")
      }
        */
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("putMFA request timeout")
      }
      validateResponse(resp.json)
    }

  }

  def getVerificationData(in: VerificationDataInput): Future[Either[YodleeException, YodleeVeriticationData]] = {
    val request = mkWSRequest("/jsonsdk/InstantVerificationDataService/getItemVerificationData")
    val data = {
      val tokens = Map("cobSessionToken" -> Seq(in.cobSessionToken),
                       "userSessionToken" -> Seq(in.userSessionToken))
      val ixs = {
        val xs = for (i <- 0 until in.itemIds.size; item = in.itemIds(i) ) yield
          Map(s"itemIds[$i]" -> Seq(item toString))
        (Map.empty[String, Seq[String]] /: xs) {_ ++ _}
      }
      tokens ++ ixs
    }

    def invalidHandler(json: JsValue): YodleeException = {
      json.validate[YodleeException].fold(
        valid = identity,
        invalid = _ => YodleeException("", "", "", "error not specified")
      )
        /*
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        case Some("InvalidCobrandConversationCredentialsException") =>
          InvalidCobrandConversationCredentialsException
        case Some("InvalidConversationCredentialsException") =>
          InvalidConversationCredentialsException
        case Some("InvalidItemException") => InvalidItemException
        case Some("IllegalArgumentValueException") => IllegalArgumentValueException
        case Some(msg) => YodleeException("", "" ,"", msg)
        case None => YodleeException("", "", "", "error not specified")
      }
        */
    }

    def validateResponse(json: JsValue): Either[YodleeException, YodleeVeriticationData] = {
      json.validate[ItemVerificationData].fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("getVerificationData request timeout")
      }
      validateResponse(resp.json)
    }
  }
}

