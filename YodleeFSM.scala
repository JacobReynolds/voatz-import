package org.nimsim.voatz.yodlee

import akka.actor._
import akka.actor.FSM._
import akka.pattern.{ ask, pipe }

import scala.async.Async.{async, await}
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps

import YodleeWS._
import yodlee._
import YodleeFSM._

object YodleeFSM {
  sealed trait YodleeMessage

  /* incoming events */
  case object YodleeIAVStart extends YodleeMessage
  case class RegistrationData(
    userCredentials: UserCredentials,
    userProfile: UserProfile,
    userPreferences: List[(String, String)]
  ) extends YodleeMessage
  case class RoutingNumber(number: Int)  extends YodleeMessage
  case class LoginFormData(
    enclosedType: String,
    accountNumber: Option[Int],
    routingNumber: Option[Int],
    credentialFields: List[CredentialFields]
  ) extends YodleeMessage
  case class MFAUserResponse(resp: UserResponse) extends YodleeMessage

  /* outgoing events */
  case object YodleeIAVStarted
  case class RequestRegistrationData(dummy: String)
  case class RequestRoutingNumber(number: Int)
  case class RequestLoginFormData(components: List[Component])
  case class RequestMFA(dummy: String)  /* could be multiple */
  case class YodleeIAVDone(status: Boolean)

  /* events initiated internally 5, some are initiated by incoming events */
  case object StartVerification extends YodleeMessage
  case class UserSignIn(login: String, pass: String) extends YodleeMessage
  case object UserSignUp extends YodleeMessage
  case object FetchLoginForm extends YodleeMessage
  case object PerformMFA extends YodleeMessage
  case object CompleteVerification extends YodleeMessage

  /* event replies by proxies */
  case class ResponseException(e: YodleeException) extends YodleeMessage
  case class AsyncResponse(e: YodleeResponse) extends YodleeMessage

  /* states */
  sealed trait YodleeState
  case object Init extends YodleeState
  case object Authenticate extends YodleeState
  case object UserLogin extends YodleeState
  case object UserRegistration extends YodleeState
  case object ContentService extends YodleeState
  case object ItemManagement extends YodleeState
  case object Verification extends YodleeState
  case object MFAVerification extends YodleeState
  case object InstantVerification extends YodleeState

  sealed trait YodleeData
  case object Uninitialized extends YodleeData
  case class Data(
      proxy: ActorRef,
      cobToken: Option[String] = None,
      userToken: Option[String] = None,
      serviceId: Option[Int] = None,
      userLogin: Option[String] = None,
      userPass: Option[String] = None,
      componentList: Option[List[Component]] = None,
      itemId: Option[Int] = None
  ) extends YodleeData
}

class YodleeFSMProxy extends Actor with ActorLogging {
  def receive = {
    case Left(e: YodleeException) => sender() ! ResponseException(e)
    case Right(r: YodleeResponse) => sender() ! AsyncResponse(r)
  }
}

class YodleeFSM extends FSM[YodleeState, YodleeData] {
  import context._

  startWith(Init, Uninitialized)
  val vLogin = "sbCobvulcan"
  val vPass = "e50e4dfa-407e-4713-9008-640be6f78fea"

  when(Init) {
    case Event(YodleeIAVStart, Uninitialized) =>
      goto(Authenticate) using Data(proxy = sender()) replying YodleeIAVStarted
  }

  when(Authenticate) {
    case Event(StartVerification, Uninitialized) =>
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
      for (t <- s.cobToken) getServiceId(t, n, false) pipeTo self
      stay

    case Event(AsyncResponse(r: ContentServiceInfo), s: Data) =>
      goto(ItemManagement) using s.copy(serviceId = Some(r.seq1.contentServiceId))
  }

  when(ItemManagement) {
    case Event(FetchLoginForm, s: Data) =>
      for (t <- s.cobToken; id <- s.serviceId) getContentServiceLoginForm(t, id) pipeTo self
      stay

    case Event(AsyncResponse(r: LoginForm), s: Data) =>
      goto(Verification) using s.copy(componentList = Some(r.componentList))
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

  when(InstantVerification) {
    case Event(CompleteVerification, s: Data) =>
      for (t <- s.cobToken; ut <- s.userToken; id <- s.itemId)
        getData(t, ut, id) pipeTo self
      stay

    case Event(AsyncResponse(r: VerificationItemInProgress), s: Data) =>
      self ! CompleteVerification
      stay

    case Event(AsyncResponse(r: VerificationItem), s: Data) =>
      r.itemVerificationInfo.requestStatus.verificationRequestStatus match {
        case "SUCCEEDED" => s.proxy ! YodleeIAVDone(true)
        case _ => s.proxy ! YodleeIAVDone(false)
      }
      goto(Init) using Uninitialized
  }

  onTransition {
    case Init -> Authenticate => self ! StartVerification
    case Authenticate -> UserLogin =>
      stateData match {
        case s: Data =>
          for (login <- s.userLogin; pass <- s.userPass) self ! UserSignIn(login, pass)
        case _ =>
      }
    case Authenticate -> UserRegistration =>
      stateData match {
        case s: Data => s.proxy ! RequestRegistrationData
        case _ =>
      }
    case UserLogin -> ContentService | UserRegistration -> ContentService =>
      stateData match {
        case s: Data => s.proxy ! RequestRoutingNumber
        case _ =>
      }
    case ContentService -> ItemManagement => self ! FetchLoginForm
    case ItemManagement -> Verification =>
      stateData match {
        case s: Data => for (xs <- s.componentList) s.proxy ! RequestLoginFormData(xs)
        case _ =>
      }
    case Verification -> InstantVerification => self ! CompleteVerification

    case Verification -> MFAVerification  =>
    case MFAVerification -> InstantVerification =>
  }

  whenUnhandled {
    case Event(f: Status.Failure, s) =>
      log.info("unhandled request {} in state {}/{}", f, stateName, s)
      stay

    case Event(e: ResponseException, s) =>
      log.info("unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()

  private def getCobrandSessionObj: Future[YodleeMessage] = {
    async {
      await { authCobrand(AuthenticateInput(vPass, vLogin)) } match {
        case Right(r: AuthenticateCobrand) => AsyncResponse(r)
        // TODO: think how to easily match on exceptions for a particular state
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  private def getLoginInfoObj(token: String, login: String, pass: String):
  Future[YodleeMessage] = {
    async {
      await { loginConsumer(LoginInput(token, login, pass)) } match {
        case Right(r: UserInfo) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  private def getUserInfoObj(token: String, data: RegistrationData):
  Future[YodleeMessage] = {
    async {
      val in = RegisterInput(token, data.userCredentials, data.userProfile, data.userPreferences)
      await { registerNewConsumer(in) } match {
        case Right(r: UserRegisterInfo) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  private def getServiceId(token: String, routingNum: Int, notrim: Boolean):
  Future[YodleeMessage] = {
    async {
      await { getContentServiceInfo(ContentServiceInfoInput(token, routingNum, notrim)) } match {
        case Right(r: ContentServiceInfo) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
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

  private def addItem(token: String, userToken: String,
                      serviceId: Int, fd: LoginFormData): Future[YodleeMessage] = {
    async {
      val in = StartVerificationInput(token, userToken, serviceId, fd.accountNumber,
                                      fd.routingNumber, fd.enclosedType, fd.credentialFields)
      await { startVerification(in) } match {
        case Right(r: IAVRefreshStatus) => AsyncResponse(r)
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

  private def getData(token: String, userToken: String, itemId: Int):
  Future[YodleeMessage] = {
    async {
      val in = VerificationDataInput(token, userToken, List(itemId))
      await { getVerificationData(in) } match {
        case Right(r) => r head match {
          case r: YodleeVerificationData => AsyncResponse(r)
        }
        case Left(e: YodleeException) => ResponseException(e)
      }
    }
  }

}
