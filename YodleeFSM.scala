package org.nimsim.voatz.yodlee

import akka.actor._
import akka.actor.FSM._

import scala.async.Async.{async, await}
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps

import YodleeWS._
import yodlee._

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
  accountNumber: Option[Int],
  routingNumber: Option[Int],
  credentialFields: List[CredentialFields]
) extends YodleeMessage
case class MFAUserResponse(resp: UserResponse) extends YodleeMessage

/* outgoing events */
case class RequestRegistrationData(dummy: String)
case class RequestRoutingNumber(number: Int)
case class RequestContentServiceLoginFormData(components: List[Component])
case class RequestMFA(dummy: String)  /* could be multiple */
case class YodleeIAVDone(status: Boolean)

/* events initiated internally 5, some are initiated by incoming events */
case object FetchCobToken extends YodleeMessage
case class UserSignIn(login: String, pass: String) extends YodleeMessage
case object UserRegister extends YodleeMessage
case object FetchContentServiceId extends YodleeMessage
case object PerformMFA extends YodleeMessage
case object CompleteVerification extends YodleeMessage
case class ResponseException(e: YodleeException) extends YodleeMessage

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
    sender: ActorRef,
    cobToken: Future[String],
    userToken: Future[String],
    servideId: Future[Int]
) extends YodleeData
case class CobrandCredentials(login: String, pass: String) extends YodleeData

class YodleeFSM extends Actor with FSM[YodleeState, YodleeData]
                              with ActorLogging {
  import context._

  startWith(Init, Uninitialized)
  val vLogin = "sbCobvulcan"
  val vPass = "e50e4dfa-407e-4713-9008-640be6f78fea"

  whenUnhandled {
    case Event(e, s) =>
      log.info("unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  onTransition {
    case Init -> Authenticate => self ! FetchCobToken
    // TODO should be a check in Mongo whether registration is required
    case Authenticate -> UserLogin => self ! UserSignIn("sbMemvulcan5", "sbMemvulcan5#123")
    case Authenticate -> UserRegistration =>
    case UserLogin -> ContentService =>
    case UserRegistration -> ContentService =>
    case ContentService -> ItemManagement =>
    case ItemManagement -> Verification =>
    case Verification -> MFAVerification  =>
    case MFAVerification -> InstantVerification =>
    case Verification -> InstantVerification =>
  }

  when(Init) {
    case Event(YodleeIAVStart, Uninitialized) =>
      goto(Authenticate) using Data(sender(), Future(""), Future(""), Future(0))
  }

  when(Authenticate) {
    case Event(FetchCobToken, s: Data) =>
      goto(UserLogin) using s.copy(cobToken = getCobrandToken)
  }

  when(UserLogin) {
    case Event(UserSignIn(uLogin, uPass), s: Data) =>
      goto(ContentService) using s.copy(userToken = getUserToken(s.cobToken, uLogin, uPass))
  }

  initialize()

  private def getCobrandToken: Future[String] = {
    async {
      await { authCobrand(AuthenticateInput(vPass, vLogin)) } match {
        case Right(r: AuthenticateCobrand) => r.cobrandConversationCredentials.sessionToken
        case Left(e: YodleeException) =>
          self ! ResponseException(e)
          throw new Exception(e toString)
      }
    }
  }

  private def getUserToken(cobToken: Future[String], login: String, pass: String): Future[String] = {
    async {
      val cobSessionToken = await { cobToken }
      await { loginConsumer(LoginInput(cobSessionToken, login, pass)) } match {
        case Right(r: UserInfo) => r.userContext.conversationCredentials.sessionToken
        case Left(e: YodleeException) =>
          self ! ResponseException(e)
          throw new Exception(e toString)
      }
    }
  }

}

/* transitions between states by Events
Init -> Authenticate by YodleeIAVStart
Authenticate -> UserLogin by internal if present in the databse
Authenticate -> UserRegistration by internal if not present in the database
UserLogin -> ContentService by internal
UserRegistrations -> Content Service by RequestRegistrationData/RegistrationInfo
ContentService -> ItemManagement by RequestRoutingNumber/RoutingNumber
ItemManagement -> Verification by RequestContentServiceLoginFormData/LoginFormData
Verification -> MFAVerification by internal based on response status
MFAVerification -> InstantVerification by RequestMFA/MFAUserResponse
Verification -> InstantVerification by internal
*/

