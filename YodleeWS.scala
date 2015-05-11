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

  def mkWSRequest(subUrl: String): WSRequest = {
    /* NB need to initialize a custom WSClient because we're not starting a Play app
     * http://carminedimascio.com/2015/02/how-to-use-the-play-ws-library-in-a-standalone-scala-app/
     */
    val client: WSClient = {
      /* NB DefaultClientConfig is called NingClientConfig in Play 2.4 */
      val config = new NingAsyncHttpClientConfigBuilder(NingWSClientConfig()).build
      val builder = new AsyncHttpClientConfig.Builder(config)
      new NingWSClient(builder.build)
    }

    val yodleeURL = "https://rest.developer.yodlee.com/services/srest/restserver/v1.0"
    val yodleeHdr1 = "Content-Type" -> "application/x-www-form-urlencoded; charset=\"utf-8\""
    val yodleeHhd2 = "Connection" -> "close"

    client.url(yodleeURL + subUrl).withHeaders(yodleeHdr1, yodleeHhd2)
  }

  def shutdown(by: String): Unit = {
    log.info(s"shutdown by $by")
    //client.close()
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
        case Some("Invalid Cobrand Credentials") => InvalidCobrandCredentialsException
        case Some("Cobrand User Account Locked") => CobrandUserAccountLockedException
        case Some(msg) => YodleeCommonException(msg)
        case None => YodleeCommonException("error not specified")
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
        case Some("InvalidCobrandContextException") => InvalidCobrandContextException
        case Some("InvalidUserCredentialsException") => InvalidUserCredentialsException
        case Some("UserUncertifiedException") => UserUncertifiedException
        case Some("UserAccountLockedException") => UserAccountLockedException
        case Some("UserUnregisteredException") => UserUnregisteredException
        case Some("UserStateChangedException") => UserStateChangedException
        case Some("YodleeAttributeException") => YodleeAttributeException
        case Some("UserSuspendedException") => UserSuspendedException
        case Some("MaxUserCountExceededException") => MaxUserCountExceededException
        case Some("UserGroupNotFoundException") => UserGroupNotFoundException
        case Some("IllegalArgumentValueException") => IllegalArgumentValueException
        case Some(msg) => YodleeCommonException(msg)
        case None => YodleeCommonException("error not specified")
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
    val data = mkData(in)
    def mkData(in: RegisterInput): Map[String, Seq[String]] = ???
    /*
    Map(
       "cobSessionToken" -> Seq(cobSessionToken),
       "userCredentials.loginName" -> Seq(userCredentials.loginName),
       "userCredentials.password" -> Seq(userCredentials.password),
       "userCredentials.objectInstanceType" -> Seq(userCredentials.objectInstanceType),
       "userProfile.emailAddress" -> Seq(userProfile.emailAddress),
       "userPreferences[0]" -> Seq() TODO: how to sent key/Value pair ASK
       "userPreferences[1]" -> Seq() TODO: how to sent key/Value pair ASK
       "userProfile.firstName" -> Seq(userProfile.firstName),
       "userProfile.lastName" -> Seq(userProfile.lastName),
       "userProfile.middleInitial" -> Seq(userProfile.middleInitial),
       "userProfile.objectInstanceType" -> Seq(userProfile.objectInstanceType),
       "userProfile.address1" -> Seq(userProfile.address1),
       "userProfile.address2" -> Seq(userProfile.address2),
       "userProfile.city" -> Seq(userProfile.city),
       "userProfile.country" -> Seq(userProfile.country)
     )
    */
    def validateResponse(json: JsValue): Either[YodleeException, YodleeRegister] = {
      json.validate[UserRegisterInfo].fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    def invalidHandler(json: JsValue): YodleeException = {
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        case Some("InvalidCobrandContextException") => InvalidCobrandContextException
        case Some("InvalidUserCredentialsException") => InvalidUserCredentialsException
        case Some("UserNameExistsException") => UserNameExistsException
        case Some("IllegalArgumentTypeException") => IllegalArgumentTypeException
        case Some("IllegalArgumentValueException") => IllegalArgumentValueException
        case Some(msg) => YodleeCommonException(msg)
        case None => YodleeCommonException("error not specified")
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
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        case Some("InvalidCobrandConversationCredentialsException") =>
          InvalidCobrandConversationCredentialsException
        case Some("InvalidRoutingNumberException") => InvalidRoutingNumberException
        case Some(msg) => YodleeCommonException(msg)
        case None => YodleeCommonException("error not specified")
      }
    }

    def validateResponse(json: JsValue): Either[YodleeException, YodleeServiceInfo] = {
      val seq1 = json.validate[ContentServiceInfoSeq1].fold(
        invalid = _ => invalidHandler(json),
        valid => identity _
      )
      val seq2 = json.validate[ContentServiceInfoSeq2].fold(
        invalid = _ => invalidHandler(json),
        valid => identity _
      )
      val seq3 = json.validate[ContentServiceInfoSeq3].fold(
        invalid = _ => invalidHandler(json),
        valid => identity _
      )

      (seq1, seq2, seq3) match {
        case (ex: YodleeException, _, _) => Left(ex)
        case (_, ex: YodleeException, _) => Left(ex)
        case (_, _, ex:  YodleeException) => Left(ex)
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
      log.info(json toString)
      Right(LoginForm("unimplemented yet")) // ASK
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

    def mkData: Map[String, Seq[String]] = ???
    val data = mkData
      /*
      Map(
      "cobSessionToken" -> Seq(cobToken),
      "userSessionToken" -> Seq(userSessionToken),
      "contentServiceId" -> Seq(contentServiceId toString),
      "accountNumber" -> Seq(accountNumber toString),
      "routingNumber" -> Seq(routingNumber toString),
      "credentialFields.enclosedType" -> Seq(credentialsEnclosedType),
      "credentialFields[0].displayName" -> Seq(cf0.displayName),
      "credentialFields[0].fieldType.typeName" -> Seq(cf0.fieldType.typeName),
      "credentialFields[0].helpText" -> Seq(cf0.helpText toString),
      "credentialFields[0].isEditable" -> Seq(cf0.isEditable toString),
      "credentialFields[0].maxlength" -> Seq(cf0.maxlength toString),
      "credentialFields[0].name" -> Seq(cf0.name),
      "credentialFields[0].size" -> Seq(cf0.size toString),
      "credentialFields[0].value" -> Seq(cf0.value),
      "credentialFields[0].valueIdentifier" -> Seq(cf0.valueIdentifier),
      "credentialFields[0].valueMask" -> Seq(cf0.valueMask),
      "credentialFields[1].displayName" -> Seq(cf1.displayName),
      "credentialFields[1].fieldType.typeName" -> Seq(cf1.fieldType.typeName),
      "credentialFields[1].helpText" -> Seq(cf1.helpText toString),
      "credentialFields[1].isEditable" -> Seq(cf1.isEditable toString),
      "credentialFields[1].maxlength" -> Seq(cf1.maxlength toString),
      "credentialFields[1].name" -> Seq(cf1.name),
      "credentialFields[1].size" -> Seq(cf1.size toString),
      "credentialFields[1].value" -> Seq(cf1.value),
      "credentialFields[1].valueIdentifier" -> Seq(cf1.valueIdentifier),
      "credentialFields[1].valueMask" -> Seq(cf1.valueMask)
    )
    */

    def validateResponse(json: JsValue): Either[YodleeException, YodleeIAVRefreshStatus] = {
      json.validate[IAVRefreshStatus].fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    def invalidHandler(json: JsValue): YodleeException = {
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
          case Some("InvalidCobrandConversationCredentialsException") =>
            InvalidCobrandConversationCredentialsException
          case Some("InvalidConversationCredentialsException") =>
            InvalidConversationCredentialsException
          case Some("ContentServiceNotFoundException") => ContentServiceNotFoundException
          case Some("IllegalArgumentValueException") => IllegalArgumentValueException
          case Some("IncompleteArgumentException") => IncompleteArgumentException
          case Some("IAVDataRequestNotSupportedException") => IAVDataRequestNotSupportedException
          case Some(msg) => YodleeCommonException(msg)
          case None => YodleeCommonException("error not specified")
      }
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
        (json \\ "errorDetail") map (_.asOpt[String]) head match {
          case Some("InvalidCobrandConversationCredentialsException") =>
            InvalidCobrandConversationCredentialsException
          case Some("InvalidConversationCredentialsException") =>
            InvalidConversationCredentialsException
          case Some("InvalidItemException") => InvalidItemException
          case Some("MFARefreshException") => MFARefreshException
          case Some(msg) => YodleeCommonException(msg)
          case None => YodleeCommonException("None")
        }
      }

    def validateResponse(json: JsValue): Either[YodleeException, YodleeMFA] = {
      val token = json.validate[MFARefreshInfoToken].fold(
        valid = identity _,
        invalid = _ => invalidHandler(json)
      )
      val image = json.validate[MFARefreshInfoImage].fold(
        valid = identity _,
        invalid = _ => invalidHandler(json)
      )
      val question = json.validate[MFARefreshInfoQuestion].fold(
        valid = identity _,
        invalid = _ =>invalidHandler(json)
      )

      (token, image, question) match {  // TODO: extremely klunky => fix
        case (v: YodleeMFA, _: YodleeException, _: YodleeException) => Right(v)
        case (_: YodleeException, v: YodleeMFA, _: YodleeException) => Right(v)
        case (_: YodleeException, _: YodleeException, v: YodleeMFA) => Right(v)
        case (e: YodleeException,
              YodleeCommonException("None"),
              YodleeCommonException("None")) => Left(e)
        case (YodleeCommonException("None"),
              e: YodleeException,
              YodleeCommonException("None")) => Left(e)
        case (YodleeCommonException("None"),
              YodleeCommonException("None"),
              e: YodleeException) => Left(e)
        case _ => Left(YodleeCommonException("error not specified"))
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

  def putMFAResponse(in: PutMFAInput): Future[Either[YodleeException, YodleeMFA]] = {
    val request = mkWSRequest("/jsonsdk/Refresh/putMFAResponse")
    def mkData = ???
    val data = mkData
    /*
    Map("cobSessionToken" -> Seq(cobToken),
                   "userSessionToken" -> Seq(userSessionToken),
                   "itemId" -> Seq(itemId toString),
                   "userResponse.objectInstanceType" -> Seq(objInstanceType)
                   // factor out into a constructor function that will map
                   // different types of userResponse to a Map
                  )
    */

    def validateResponse(json: JsValue): Either[YodleeException, YodleeMFA] = {
      json.validate[MFAPutResponse].fold(
        valid = Right(_),
        invalid = _ => Left(invalidHandler(json))
      )
    }

    def invalidHandler(json: JsValue): YodleeException = {
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        case Some("InvalidCobrandConversationCredentialsException") =>
          InvalidCobrandConversationCredentialsException
        case Some("InvalidConversationCredentialsException") =>
          InvalidConversationCredentialsException
        case Some("InvalidItemException") => InvalidItemException
        case Some("MFARefreshException") => MFARefreshException
        case Some("IllegalArgumentValueException") => IllegalArgumentValueException
        case Some(msg) => YodleeCommonException(msg)
        case None => YodleeCommonException("None")
      }
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
    val data = mkData
    def mkData = ???
    /*
    Map("cobSessionToken" -> Seq(cobSessionToken),
                   "userSessionToken" -> Seq(userSessionToken),
                   "itemIds[0]" -> Seq(itemIds(0) toString)) //TODO factor our into a function
    */
    def invalidHandler(json: JsValue): YodleeException = {
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
        case Some("InvalidCobrandConversationCredentialsException") =>
          InvalidCobrandConversationCredentialsException
        case Some("InvalidConversationCredentialsException") =>
          InvalidConversationCredentialsException
        case Some("InvalidItemException") => InvalidItemException
        case Some("IllegalArgumentValueException") => IllegalArgumentValueException
        case Some(msg) => YodleeCommonException(msg)
        case None => YodleeCommonException("error not specified")
      }
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

object Client extends App {
  /* TODOs:
   * 3. Return Future[Either[YodleeException, YodleeSomeResponse]] => scalaz
   * 4. Decide what to do with LoginForms - convert to case classes
   * 5. Introduce functions to convert input objects into data for POST req
   * 7. Factor our async/await blocks
   * 9. Factor out client into unit tests and general client test that goes
   * through the whole verification cycle: test for FSM
   */

  import YodleeWS._

  /* Client Code */
  def start: Unit = {

    /* step 1 */
    val cobToken = {
      val auth = authenticateStep
      for (exc <- auth.failed) {
        log.info(exc.getMessage)
        shutdown("Step1")
      }
      auth
    }

    /* step 2b */
    val userToken = cobToken flatMap { t =>
      val userSessionToken = loginStep(t)
      for (exc <- userSessionToken.failed) {
        log.info(exc.getMessage)
        shutdown("Step 2b")
      }
      userSessionToken
    }

    /* step3 */
    val serviceID = cobToken flatMap { t =>
      val contentServiceId = serviceInfoStep(t)
      for (exc <- contentServiceId.failed) {
        log.info(exc.getMessage)
        shutdown("Step 3")
      }
      contentServiceId
    }

    val loginFormVal = {
      val form = (for {
        token: String <- cobToken
        id: Int <- serviceID
      } yield loginFormStep(token, id)) flatMap(identity)

      for (exc <- form.failed) {
        log.info(exc.getMessage)
        shutdown("Step 4")
      }
      form
    }

    cobToken foreach { tn => log.info(s"cobToken = $tn") }
    userToken foreach { ut => log.info(s"userToken = $ut") }
    serviceID foreach { id => log.info(s"contentServiceId = $id") }
    loginFormVal map { v => log.info(s"form = $v"); v } andThen {
      case _ => shutdown("Step Final")
    }

  }

  def authenticateStep: Future[String] = {
    val voatzLogin = "sbCobvulcan"
    val voatzPass = "e50e4dfa-407e-4713-9008-640be6f78fea"
    val authRes = authCobrand(AuthenticateInput(voatzLogin, voatzPass))

    /* get sessionToken or the exception */
    authRes map { _ match {
      case Left(exp) => throw new Exception(exp toString)
      case Right(r @ (_: AuthenticateCobrand)) =>
        r.cobrandConversationCredentials.sessionToken
      }
    }
  }

  def loginStep(token: String): Future[String] = {
    val username = "sbMemvulcan2"
    val password = "sbMemvulcan2#123"
    val loginResp = loginConsumer(LoginInput(token, username, password))

    loginResp map { _ match {
        case resp: UserInfo =>
          resp.userContext.conversationCredentials.sessionToken
        case exp: YodleeLogin => throw new Exception(exp toString)
      }
    }
  }

  def serviceInfoStep(token: String): Future[Int] = {
    val dagRoutingNumber = 999999989
    val serviceInfo = getContentServiceInfo(ContentServiceInfoInput(token,
                                                                    dagRoutingNumber,
                                                                    true))
    serviceInfo map { _ match {
        case resp: ContentServiceInfo => resp.seq1.contentServiceId
        case exp: YodleeServiceInfo => throw new Exception(exp toString)
      }
    }
  }

  def loginFormStep(token: String, id: Int): Future[String] = {
    val form = getLoginForm(LoginFormInput(token, id))
    form map { _ match {
        case resp: LoginForm => resp.dummy
        case exp: YodleeLoginFormException => throw new Exception(exp toString)
      }
    }
  }

  start
}
