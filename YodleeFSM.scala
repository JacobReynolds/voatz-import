package org.nimsim.voatz.yodlee

import akka.actor._
import akka.actor.FSM._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout

import java.util.concurrent.TimeUnit
import scala.async.Async.{async, await}
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps


class YodleeWSProxy extends Actor with ActorLogging {
  import YodleeIAVFSM._
  def receive = {
    case Left(e: YodleeException) => sender() ! ResponseException(e)
    case Right(r: YodleeResponse) => sender() ! AsyncResponse(r)
  }
}

object Worker extends App {
  import YodleeIAV._
  implicit val t = Timeout(100, TimeUnit.SECONDS)

  val system = ActorSystem()
  val proxy = system.actorOf(YodleeIAV.props)
  import scala.concurrent.ExecutionContext.Implicits.global
  val res = (proxy ? ExtendedVerification).mapTo[Boolean]
  res foreach { r =>
    println(s"verification result = $r")
    system shutdown
  }
}

object YodleeIAV {
  case object ExtendedVerification
  def props: Props = Props(new YodleeIAV)
}

class YodleeIAV extends Actor with ActorLogging {
  import YodleeIAVFSM._
  import YodleeIAV._

  val fsm = context.actorOf(YodleeIAVFSM.props, "FSM")
  def receive = dispatcher(sender())

  def dispatcher(worker: ActorRef): Receive = {
    case ExtendedVerification => fsm ! IAVStart

    case RequestSiteOrRTN => sender() ! RoutingNumber(910080000)

    case r: RequestLoginFormData =>
      sender() ! LoginFormData("com.yodlee.common.FieldInfoSingle", List(
        CredentialFields("User Name", FieldType("TEXT"), 92430, true, 40,
          "LOGIN", 20, "vulcan.BankTokenFMPA1", "LOGIN", "LOGIN_FIELD"),
        CredentialFields("Password", FieldType("IF_PASSWORD"), 92429, true,
          40, "PASSWORD", 20, "BankTokenFMPA1", "PASSWORD", "LOGIN_FIELD")))

    case RequestMFA(fi) =>
      sender() ! UserResponse(MFATokenResponse("com.yodlee.core.mfarefresh.MFATokenResponse",
        "123456"))

    case IAVDone(status) => worker ! status
  }
}

object YodleeIAVFSM {
  sealed trait IAVMessage

  /* incoming events */
  case object IAVStart extends IAVMessage
  case class RegistrationData(
    userCredentials: UserCredentials,
    userProfile: UserProfile,
    userPreferences: List[(String, String)]
  ) extends IAVMessage
  case class RoutingNumber(number: Int) extends IAVMessage
  case class BankSite(bank: String) extends IAVMessage
  case class LoginFormData(
    enclosedType: String,
    credentialFields: List[CredentialFields]
  ) extends IAVMessage
  case class UserResponse(resp: MFAUserResponse) extends IAVMessage

  /* outgoing events */
  case object RequestRegistrationData extends IAVMessage
  case object RequestSiteOrRTN extends IAVMessage
  case class RequestLoginFormData(components: List[Component]) extends IAVMessage
  case class RequestMFA(fieldInfo: FieldInfo) extends IAVMessage
  case class IAVDone(status: Boolean) extends IAVMessage

  /* events initiated internally 5, some are initiated by incoming events */
  case object StartVerification extends IAVMessage
  case class UserSignIn(login: String, pass: String) extends IAVMessage
  case object UserSignUp extends IAVMessage
  case object PerformMFA extends IAVMessage
  case object CompleteVerification extends IAVMessage
  //case object FetchLoginForm extends IAVMessage

  /* event replies by WS actor */
  case class ResponseException(e: YodleeException) extends IAVMessage
  case class AsyncResponse(e: YodleeResponse) extends IAVMessage

  /* states */
  sealed trait IAVState
  case object Init extends IAVState
  case object Authenticate extends IAVState
  case object UserLogin extends IAVState
  case object UserRegistration extends IAVState
  case object ContentService extends IAVState
  case object ItemManagement extends IAVState
  case object Verification extends IAVState
  case object MFAVerification extends IAVState
  case object InstantVerification extends IAVState

  sealed trait IAVData
  case object Uninitialized extends IAVData
  case class Data(
      proxy: ActorRef,
      cobToken: Option[String] = None,
      userToken: Option[String] = None,
      serviceId: Option[Int] = None,
      userLogin: Option[String] = None,
      userPass: Option[String] = None,
      componentList: Option[List[Component]] = None,
      itemId: Option[Int] = None,
      retry: Option[Boolean] = None
  ) extends IAVData

  def props: Props = Props(new YodleeIAVFSM)
}

class YodleeIAVFSM extends FSM[YodleeIAVFSM.IAVState, YodleeIAVFSM.IAVData] {
  import context._
  import YodleeIAVFSM._
  import YodleeMFAFSM._
  import YodleeWS._

  startWith(Init, Uninitialized)

  when(Init) {
    case Event(IAVStart, Uninitialized) =>
      goto(Authenticate) using Data(proxy = sender())
  }

  when(Authenticate) {
    case Event(StartVerification, s: Data) =>
      getCobrandSessionObj pipeTo self // introduce a proxy actor which will handle
      //Status.Failure and return a YodleeException to match in this state
      stay

    case Event(AsyncResponse(r: AuthenticateCobrand), s: Data) =>
      val uLogin = "sbMemvulcan5"
      val uPass = "sbMemvulcan5#123"
      val state = s.copy(cobToken = Some(r.cobrandConversationCredentials.sessionToken))
      // TODO should be a check in Mongo whether registration is required
      if (true) {
        goto(UserLogin) using state.copy(userLogin = Some(uLogin), userPass = Some(uPass))
      } else {
        goto(UserRegistration) using state
      }
  }

  when(UserLogin) {
    case Event(UserSignIn(uLogin, uPass), s: Data) =>
      for (t <- s.cobToken) getLoginInfoObj(t, uLogin, uPass) pipeTo self
      stay

    case Event(AsyncResponse(r: UserInfo), s: Data) =>
      val state = s.copy(userToken = Some(r.userContext.conversationCredentials.sessionToken))
      goto(ContentService) using state
  }

  when(UserRegistration) {
    case Event(d: RegistrationData, s: Data) =>
      for (t <- s.cobToken) getUserInfoObj(t, d) pipeTo self
      stay

    case Event(AsyncResponse(r: UserRegisterInfo), s: Data) =>
      val state = s.copy(userToken = Some(r.userContext.conversationCredentials.sessionToken))
      goto(ContentService) using state
  }

  when(ContentService) {
    case Event(RoutingNumber(n), s: Data) =>
      for (t <- s.cobToken) getServiceId(t, n, true) pipeTo self
      stay

    case Event(AsyncResponse(r: ContentServiceInfo), s: Data) =>
      goto(Verification) using s.copy(serviceId = Some(r.seq1.contentServiceId),
                                      componentList = Some(r.seq2.loginForm.componentList))
  }

  when(Verification) {
    case Event(r: LoginFormData, s: Data) =>
      for (t <- s.cobToken; ut <- s.userToken; id <- s.serviceId)
        addItem(t, ut, id, r) pipeTo self
      stay

    case Event(AsyncResponse(r: IAVRefreshStatus), s: Data) =>
      val state = s.copy(itemId = Some(r.itemId))
      if (r.refreshStatus.status == 4)
        goto(InstantVerification) using state
      else
        goto(MFAVerification) using state
  }

  when(MFAVerification) {
    case Event(PerformMFA, s: Data) =>
      for (t <- s.cobToken; ut <- s.userToken; itemId <- s.itemId)
        (context.actorOf(YodleeMFAFSM.props(self, t, ut, itemId), "MFAFSM")) ! MFAPending
      stay

    case Event(msg: RequestMFA, s: Data) =>
      s.proxy forward msg
      stay

    case Event(MFASuccess, s: Data) => goto(InstantVerification)
    case Event(MFAFailure(error: Int), s: Data) =>
      self ! ResponseException(YodleeSimpleException(s"MFA failed: $error"))
      stay
  }

  when(InstantVerification) {
    case Event(CompleteVerification, s: Data) =>
      for (t <- s.cobToken; ut <- s.userToken; id <- s.itemId)
        getData(t, ut, id) pipeTo self
      stay

    case Event(AsyncResponse(r: VerificationItemInProgress), s: Data) =>
      self ! CompleteVerification // NB: remove in akka >= 2.4
      goto(stateName)

    case Event(AsyncResponse(r: VerificationItem), s: Data) =>
      r.itemVerificationInfo.requestStatus.verificationRequestStatus match {
        case "SUCCEEDED" => s.proxy ! IAVDone(true)
        case _ => s.proxy ! IAVDone(false)
      }
      goto(Init) using Uninitialized
  }

  onTransition {
    case Init -> Authenticate => self ! StartVerification
    case Authenticate -> UserLogin =>
      nextStateData match {
        case s: Data =>
          for (login <- s.userLogin; pass <- s.userPass) self ! UserSignIn(login, pass)
        case _ =>
      }
    case Authenticate -> UserRegistration =>
      nextStateData match {
        case s: Data => s.proxy ! RequestRegistrationData
        case _ =>
      }
    case UserLogin -> ContentService | UserRegistration -> ContentService =>
      nextStateData match {
        case s: Data => s.proxy ! RequestSiteOrRTN
        case _ =>
      }
    case ContentService -> Verification =>
      nextStateData match {
        case s: Data => for (xs <- s.componentList) s.proxy ! RequestLoginFormData(xs)
        case _ =>
      }
    case Verification -> MFAVerification  => self ! PerformMFA
    case _ -> InstantVerification => log.info("FIRE"); self ! CompleteVerification
  }

  whenUnhandled {
    case Event(f: Status.Failure, s) =>
      log.info("Future failed {} in state {}/{}", f, stateName, s)
      stay

    case Event(e: ResponseException, s) =>
      log.info("Yodlee Exception {} in state {}/{}", e, stateName, s)
      stay

    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stop
  }

  initialize()

  private def getCobrandSessionObj: Future[IAVMessage] = {
    val vLogin = "sbCobvulcan"
    val vPass = "e50e4dfa-407e-4713-9008-640be6f78fea"
    async {
      await { authCobrand(AuthenticateInput(vLogin, vPass)) } match {
        case Right(r: AuthenticateCobrand) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  private def getLoginInfoObj(token: String, login: String, pass: String):
  Future[IAVMessage] = {
    async {
      await { loginConsumer(LoginInput(token, login, pass)) } match {
        case Right(r: UserInfo) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  private def getUserInfoObj(token: String, data: RegistrationData):
  Future[IAVMessage] = {
    async {
      val in = RegisterInput(token, data.userCredentials, data.userProfile, data.userPreferences)
      await { registerNewConsumer(in) } match {
        case Right(r: UserRegisterInfo) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  private def getServiceId(token: String, routingNum: Int, notrim: Boolean):
  Future[IAVMessage] = {
    async {
      await { getContentServiceInfo(ContentServiceInfoInput(token, routingNum, notrim)) } match {
        case Right(r: ContentServiceInfo) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  private def addItem(token: String, userToken: String,
                      serviceId: Int, fd: LoginFormData): Future[IAVMessage] = {
    async {
      val in = StartVerificationInput(
        token, userToken, fd.enclosedType, fd.credentialFields, IAVRequest(serviceId)
      )
      await { startVerification(in) } match {
        case Right(r: IAVRefreshStatus) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  private def getData(token: String, userToken: String, itemId: Int):
  Future[IAVMessage] = {
    async {
      val in = VerificationDataInput(token, userToken, List(itemId))
      await { getVerificationData(in) } match {
        case Right(xs) => xs head match {
          case r: YodleeVerificationData => AsyncResponse(r)
        }
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  /* was used in old API
  onTransition {
    case ContentService -> ItemManagement => self ! FetchLoginForm
  }
  private def getContentServiceLoginForm(token: String, serviceId: Int):
  Future[YodleeMessage] = {
    async {
      await { getLoginForm(LoginFormInput(token, serviceId)) } match {
        case Right(r: LoginForm) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  when(ItemManagement) {
    case Event(FetchLoginForm, s: Data) =>
      for (t <- s.cobToken; id <- s.serviceId) getContentServiceLoginForm(t, id) pipeTo self
      stay

    case Event(AsyncResponse(r: LoginForm), s: Data) =>
      goto(Verification) using s.copy(componentList = Some(r.componentList))
  }
  */
}

object YodleeMFAFSM {
  sealed trait MFAData
  case object Uninit extends MFAData

  sealed trait MFAState
  case object GetMFA extends MFAState
  case object PutMFA extends MFAState

  /* event replies by MFA FSM */
  sealed trait MFAMessage
  case object MFAPending extends MFAMessage
  case object MFASuccess extends MFAMessage
  case class MFAFailure(errorCode: Int) extends MFAMessage

  def props(fsm: ActorRef, token: String, userToken: String, itemId: Int): Props = {
    Props(new YodleeMFAFSM(fsm, token, userToken, itemId))
  }
}

class YodleeMFAFSM(fsm: ActorRef, token: String, userToken: String, itemId: Int)
  extends FSM[YodleeMFAFSM.MFAState, YodleeMFAFSM.MFAData] {
  import context._
  import YodleeIAVFSM._
  import YodleeMFAFSM._
  import YodleeWS._

  startWith(GetMFA, Uninit)

  when(GetMFA) {
    case Event(MFAPending, _) =>
      getMFA(token, userToken, itemId) pipeTo self
      stay

    case Event(AsyncResponse(r: MFARefreshDone), _) =>
      fsm ! (if (r.errorCode == 0) MFASuccess else MFAFailure(r.errorCode))
      stop()

    case Event(AsyncResponse(r: MFARefreshPending), _) =>
      self ! MFAPending
      stay

    case Event(AsyncResponse(resp: MFARefreshFields), _) =>
      resp match {
        case r: MFARefreshInfoToken => fsm ! RequestMFA(r.fieldInfo)
        case r: MFARefreshInfoImage => fsm ! RequestMFA(r.fieldInfo)
        case r: MFARefreshInfoQuestion => fsm ! RequestMFA(r.fieldInfo)
      }
      goto(PutMFA)
  }

  when(PutMFA) {
    case Event(UserResponse(resp: MFAUserResponse), _) =>
      putMFA(token, userToken, itemId, resp) pipeTo self
      stay

    case Event(AsyncResponse(r: MFAPutResponse), _) =>
      if (r.primitiveObj == true) {
        self ! MFAPending
        goto(GetMFA)
      } else {
        fsm ! MFAFailure(0)
        stop()
      }
  }

  initialize()

  private def getMFA(token: String, userToken: String, itemId: Int):
  Future[IAVMessage] = {
    async {
      await { getMFAResponse(GetMFAInput(token, userToken, itemId)) } match {
        case Right(r: YodleeMFA) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  // TODO change it to get the data
  private def putMFA(token: String, userToken: String, itemId: Int, r: MFAUserResponse):
  Future[IAVMessage] = {
    async {
      val p = await { putMFARequest(PutMFAInput(GetMFAInput(token, userToken, itemId), r)) }
      p match {
        case Right(r: YodleeMFA) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

}
