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

  def shutdown(by: String): Unit = {
    log.info(s"shutdown by $by")
    client.close()
    system.shutdown
  }

  /* Step 1 of the API */
  def authenticateCobrand(cobrandLogin: String,
                          cobrandPassword: String): Future[YodleeAuthenticate] = {
    val url = yodleeURL + "/authenticate/coblogin"
    val request = client.url(url).withHeaders(yodleeHdr1, yodleeHhd2)
    val data = Map("cobrandLogin" -> Seq(cobrandLogin),
                   "cobrandPassword" -> Seq(cobrandPassword))

    def validateResponse(json: JsValue): YodleeAuthenticate = {
      json.validate[AuthenticateCobrand].fold(valid = identity,
        invalid = _ => (json \\ "errorDetail") map (_.asOpt[String]) head match {
          case Some("Invalid Cobrand Credentials") => InvalidCobrandCredentialsException
          case Some("Cobrand User Account Locked") => CobrandUserAccountLockedException
          case Some(msg) => YodleeAuthenticateException(msg)
          case None => YodleeAuthenticateException("error not specified")
        })
    }

    async {
      implicit val timeout: FiniteDuration = 3000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("authenticate request timeout")
      }
      validateResponse(resp.json)
    }
  }

  def loginConsumer(cobToken: String,
                    userLogin: String,
                    userPass: String): Future[YodleeLogin] = {
    val url = yodleeURL + "/authenticate/login"
    val request = client.url(url).withHeaders(yodleeHdr1, yodleeHhd2)
    val data = Map("cobSessionToken" -> Seq(cobToken),
                   "login" -> Seq(userLogin),
                   "password" -> Seq(userPass))

    def validateResponse(json: JsValue): YodleeLogin = {
      json.validate[UserInfo].fold(valid = identity,
        invalid = _ => (json \\ "errorDetail") map (_.asOpt[String]) head match {
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
          case Some(msg) => YodleeLoginException(msg)
          case None => YodleeLoginException("error not specified")
        })
    }

    async {
      implicit val timeout: FiniteDuration = 3000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("login request timeout")
      }
      validateResponse(resp.json)
    }
  }

  def registerNewConsumer(cobSessionToken: String,
                          userCredentials: UserCredentials,
                          userProfile: UserProfile,
                          userPreferences: Map[String, String]): Future[YodleeLogin] = {
    val url = yodleeURL + "/jsonsdk/UserRegistration/register3"
    val request = client.url(url).withHeaders(yodleeHdr1, yodleeHhd2)
    val data = Map(
      "cobSessionToken" -> Seq(cobSessionToken),

    /*
      "userCredentials" -> Seq(Map("loginName" -> Seq(loginName),
                                    "password" -> Seq(password)))
      */
       "userCredentials.loginName" -> Seq(userCredentials.loginName),
       "userCredentials.password" -> Seq(userCredentials.password),
       "userCredentials.objectInstanceType" -> Seq(userCredentials.objectInstanceType),
       "userProfile.emailAddress" -> Seq(userProfile.emailAddress),
       //"userPreferences[0]" -> Seq() TODO: how to sent key/Value pair ASK
       "userProfile.firstName" -> Seq(userProfile.firstName),
       "userProfile.lastName" -> Seq(userProfile.lastName),
       "userProfile.middleInitial" -> Seq(userProfile.middleInitial),
       "userProfile.objectInstanceType" -> Seq(userProfile.objectInstanceType),
       "userProfile.address1" -> Seq(userProfile.address1),
       "userProfile.address2" -> Seq(userProfile.address2),
       "userProfile.city" -> Seq(userProfile.city),
       "userProfile.country" -> Seq(userProfile.country)
     )

    def validateResponse(json: JsValue): YodleeLogin = {
      json.validate[UserRegisterInfo].fold(valid = identity,
        invalid = _ => (json \\ "errorDetail") map (_.asOpt[String]) head match {
          case Some("InvalidCobrandContextException") =>
            InvalidCobrandContextException
          case Some("InvalidUserCredentialsException") =>
            InvalidUserCredentialsException
          case Some("UserNameExistsException") => UserNameExistsException
          case Some("IllegalArgumentTypeException") => IllegalArgumentTypeException
          case Some("IllegalArgumentValueException") => IllegalArgumentValueException
          case Some(msg) => YodleeLoginException(msg)
          case None => YodleeLoginException("error not specified")
        })
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("getVerificationData request timeout")
      }
      validateResponse(resp.json)
    }
  }

  def getContentServiceInfo(cobToken: String,
                            routingNumber: Int,
                            notrim: Boolean): Future[YodleeServiceInfo] = {
    val url = yodleeURL + "/jsonsdk/RoutingNumberService/getContentServiceInfoByRoutingNumber"
    val request = client.url(url).withHeaders(yodleeHdr1, yodleeHhd2)
    val data = Map("cobSessionToken" -> Seq(cobToken),
                   "routingNumber" -> Seq(routingNumber toString),
                   "notrim" -> Seq(notrim toString))

    def invalidHandler(json: JsValue):
      Seq[(JsPath, Seq[ValidationError])] => YodleeServiceInfo = { _ =>
      (json \\ "errorDetail") map (_.asOpt[String]) head match {
          case Some("InvalidCobrandConversationCredentialsException") =>
            InvalidCobrandConversationCredentialsException
          case Some("InvalidRoutingNumberException") =>
            InvalidRoutingNumberException
          case Some(msg) => YodleeServiceInfoException(msg)
          case None => YodleeServiceInfoException("error not specified")
        }
    }

    def validateResponse(json: JsValue): YodleeServiceInfo = {
      val seq1 = json.validate[ContentServiceInfoSeq1].fold(invalidHandler(json), identity)
      val seq2 = json.validate[ContentServiceInfoSeq2].fold(invalidHandler(json), identity)
      val seq3 = json.validate[ContentServiceInfoSeq3].fold(invalidHandler(json), identity)
      (seq3, seq2, seq3) match {
        case (ex: YodleeServiceInfo, _, _) => ex
        case (_, ex: YodleeServiceInfo, _) => ex
        case (_, _, ex: YodleeServiceInfo) => ex
        case _ => ContentServiceInfo(seq1.asInstanceOf[ContentServiceInfoSeq1],
                                     seq2.asInstanceOf[ContentServiceInfoSeq2],
                                     seq3.asInstanceOf[ContentServiceInfoSeq3])
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

  def getLoginForm(cobToken: String, serviceId: Int): Future[YodleeLoginForm] = {
    val url = yodleeURL + "/jsonsdk/ItemManagement/getLoginFormForContentService"
    val request = client.url(url).withHeaders(yodleeHdr1, yodleeHhd2)
    val data = Map("cobSessionToken" -> Seq(cobToken),
                   "contentServiceId" -> Seq(serviceId toString))

    def validateResponse(json: JsValue): YodleeLoginForm = {
      log.info(json toString)
      LoginForm("unimplemented yet") // ASK
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("loginForm request timeout")
      }
      validateResponse(resp.json)
    }
  }

  def addItemAndStartVerification(
    cobToken: String,
    userSessionToken: String,
    contentServiceId: Int,
    accountNumber: Int,
    routingNumber: Int,
    credentialsEnclosedType: String,
    credentialFields: (CredentialFields, CredentialFields)): Future[YodleeIAVRefreshStatus] = {

    val url = yodleeURL +
    "/jsonsdk/ExtendedInstantVerificationDataService/addItemAndStartVerificationDataRequest"
    val (cf0, cf1) = credentialFields
    val data = Map(
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

    val request = client.url(url).withHeaders(yodleeHdr1, yodleeHhd2)
    def validateResponse(json: JsValue): YodleeIAVRefreshStatus = {
      json.validate[IAVRefreshStatus].fold(valid = identity,
        invalid = _ => (json \\ "errorDetail") map (_.asOpt[String]) head match {
          //case Some("InvalidCobrandConversationCredentialsException") =>
            //InvalidCobrandConversationCredentialsException
          case Some("InvalidConversationCredentialsException") =>
            InvalidConversationCredentialsException
          //case Some("ContentServiceNotFoundException") => ContentServiceNotFoundException
          //case Some("IllegalArgumentValueException") => IllegalArgumentValueException
          case Some("IncompleteArgumentException") => IncompleteArgumentException
          case Some("IAVDataRequestNotSupportedException") => IAVDataRequestNotSupportedException
          case Some(msg) => YodleeIAVRefreshStatusException(msg)
          case None => YodleeIAVRefreshStatusException("error not specified")
      })
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("start verification request timeout")
      }
      validateResponse(resp.json)
    }

  }

  def getMFAResponse(cobToken: String,
                     userSessionToken: String,
                     itemId: Int): Future[YodleeMFA] = {
    val url = yodleeURL + "/jsonsdk/Refresh/getMFAResponse"
    val request = client.url(url).withHeaders(yodleeHdr1, yodleeHhd2)
    val data = Map("cobSessionToken" -> Seq(cobToken),
                   "userSessionToken" -> Seq(userSessionToken),
                   "itemId" -> Seq(itemId toString))

    def invalidHandler(json: JsValue):
      Seq[(JsPath, Seq[ValidationError])] =>YodleeMFA = { _ =>
        (json \\ "errorDetail") map (_.asOpt[String]) head match {
          //case Some("InvalidCobrandConversationCredentialsException") =>
            //InvalidCobrandConversationCredentialsException
          //case Some("InvalidConversationCredentialsException") =>
            //InvalidConversationCredentialsException
          case Some("InvalidItemException") => InvalidItemException
          case Some("MFARefreshException") => MFARefreshException
          case Some(msg) =>YodleeMFAException(msg)
          case None =>YodleeMFAException("None")
        }
      }

    def validateResponse(json: JsValue):YodleeMFA = {
      val token = json.validate[MFARefreshInfoToken].fold(invalidHandler(json), identity)
      val image = json.validate[MFARefreshInfoImage].fold(invalidHandler(json), identity)
      val question = json.validate[MFARefreshInfoQuestion].fold(invalidHandler(json), identity)
      (token, image, question) match {  // TODO: extremely klunky => fix
        case (v: MFARefreshInfoToken,
             YodleeMFAException("None"),
             YodleeMFAException("None")) => v
        case (YodleeMFAException("None"),
              v: MFARefreshInfoImage,
             YodleeMFAException("None")) => v
        case (YodleeMFAException("None"),
             YodleeMFAException("None"),
              v: MFARefreshInfoQuestion) => v
        case (e:YodleeMFA,
             YodleeMFAException("None"),
             YodleeMFAException("None")) => e
        case (YodleeMFAException("None"),
              e:YodleeMFA,
             YodleeMFAException("None")) => e
        case (YodleeMFAException("None"),
             YodleeMFAException("None"),
              e:YodleeMFA) => e
        case _ =>YodleeMFAException("error not specified")
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

  def putMFAResponse(cobToken: String,
                     userSessionToken: String,
                     itemId: Int,
                     objInstanceType: String,
                     userResponse: MFAUserResponse,
                     userResponseToke: Int): Future[YodleeMFA] = {
    val url = yodleeURL + "/jsonsdk/Refresh/putMFAResponse"
    val request = client.url(url).withHeaders(yodleeHdr1, yodleeHhd2)
    val data = Map("cobSessionToken" -> Seq(cobToken),
                   "userSessionToken" -> Seq(userSessionToken),
                   "itemId" -> Seq(itemId toString),
                   "userResponse.objectInstanceType" -> Seq(objInstanceType)
                   // factor out into a constructor function that will map
                   // different types of userResponse to a Map
                  )

    def invalidHandler(json: JsValue):
      Seq[(JsPath, Seq[ValidationError])] => YodleeMFA = { _ =>
        (json \\ "errorDetail") map (_.asOpt[String]) head match {
          //case Some("InvalidCobrandConversationCredentialsException") =>
            //InvalidCobrandConversationCredentialsException
          //case Some("InvalidConversationCredentialsException") =>
            //InvalidConversationCredentialsException
          case Some("InvalidItemException") => InvalidItemException
          case Some("MFARefreshException") => MFARefreshException
          //case Some("IllegalArgumentValueException") => IllegalArgumentValueException
          case Some(msg) => YodleeMFAException(msg)
          case None => YodleeMFAException("None")
        }
      }

    def validateResponse(json: JsValue): YodleeMFA = {
      json.validate[MFAPutResponse].fold(invalidHandler(json), identity)
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("putMFA request timeout")
      }
      validateResponse(resp.json)
    }

  }

  def getVerificationData(cobSessionToken: String,
                          userSessionToken: String,
                          itemIds: List[Int]): Future[YodleeVeriticationData] = {
    val url = yodleeURL + "/jsonsdk/InstantVerificationDataService/getItemVerificationData"
    val request = client.url(url).withHeaders(yodleeHdr1, yodleeHhd2)
    val data = Map("cobSessionToken" -> Seq(cobSessionToken),
                   "userSessionToken" -> Seq(userSessionToken),
                   "itemIds[0]" -> Seq(itemIds(0) toString)) //TODO factor our into a function

    def invalidHandler(json: JsValue):
      Seq[(JsPath, Seq[ValidationError])] => YodleeVeriticationData = { _ =>
        (json \\ "errorDetail") map (_.asOpt[String]) head match {
          //case Some("InvalidCobrandConversationCredentialsException") =>
            //InvalidCobrandConversationCredentialsException
          //case Some("InvalidConversationCredentialsException") =>
            //InvalidConversationCredentialsException
          //case Some("InvalidItemException") => InvalidItemException
          //case Some("IllegalArgumentValueException") => IllegalArgumentValueException
          case Some(msg) => YodleeVeriticationDataException(msg)
          case None => YodleeVeriticationDataException("error not specified")
        }
    }

    def validateResponse(json: JsValue): YodleeVeriticationData = {
      json.validate[ItemVerificationData].fold(invalidHandler(json), identity)
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
   * 1. Factor out all Exceptions into YodleeException trait
   * 2. Introduce YodleeCommonException(msg: String) to handle unknown cases
   * 3. Return Future[Either[YodleeException, YodleeSomeResponse]] => scalaz
   * 4. Decide what to do with LoginForms Fix
   * 5. Introduce functions to convert input objects into data for POST req
   * 6. Factor out clients into common mkClient function
   * 7. Factor our async/await blocks
   * 8. Divide into multiple files: yodleeModels, userResponses(?), this
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

    val authRes = authenticateCobrand(voatzLogin, voatzPass)

    /* get sessionToken or the exception */
    authRes map { _ match {
        case resp: AuthenticateCobrand =>
          resp.cobrandConversationCredentials.sessionToken
        case exp: YodleeAuthenticate => throw new Exception(exp toString)
      }
    }
  }

  def loginStep(token: String): Future[String] = {
    val username = "sbMemvulcan2"
    val password = "sbMemvulcan2#123"
    val loginResp = loginConsumer(token, username, password)

    loginResp map { _ match {
        case resp: UserInfo =>
          resp.userContext.conversationCredentials.sessionToken
        case exp: YodleeLogin => throw new Exception(exp toString)
      }
    }
  }

  def serviceInfoStep(token: String): Future[Int] = {
    val dagRoutingNumber = 999999989
    val serviceInfo = getContentServiceInfo(token, dagRoutingNumber, true)
    serviceInfo map { _ match {
        case resp: ContentServiceInfo => resp.seq1.contentServiceId
        case exp: YodleeServiceInfo => throw new Exception(exp toString)
      }
    }
  }

  def loginFormStep(token: String, id: Int): Future[String] = {
    val form = getLoginForm(token, id)
    form map { _ match {
        case resp: LoginForm => resp.dummy
        case exp: YodleeLoginFormException => throw new Exception(exp toString)
      }
    }
  }

  start
}
