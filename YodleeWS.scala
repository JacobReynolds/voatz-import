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
import play.api.data.validation._
import org.slf4j.LoggerFactory

import org.voatz.AsyncUtils._

//sealed trait YodleeCommon
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

case class ConversationCredentials(sessionToken: String)
object ConversationCredentials {
  implicit val reads = Json.reads[ConversationCredentials]
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

sealed trait YodleeAuthenticate /* Step 1 */
case object InvalidCobrandCredentialsException extends YodleeAuthenticate
case object CobrandUserAccountLockedException extends YodleeAuthenticate
case class YodleeAuthenticateException(msg: String) extends YodleeAuthenticate
case class AuthenticateCobrand(
  cobrandId: Int,
  channelId: Int,
  locale: String,
  tncVersion: Int,
  applicationId: String,
  cobrandConversationCredentials: ConversationCredentials,
  preferenceInfo: PreferenceInfo
) extends YodleeAuthenticate
object AuthenticateCobrand {
  implicit val reads = Json.reads[AuthenticateCobrand]
}

sealed trait YodleeLogin /* Step 2b */
case class UserContext(
  conversationCredentials: ConversationCredentials,
  valid: Boolean,
  isPasswordExpired: Boolean,
  cobrandId: Int,
  channelId: Int,
  locale: String,
  tncVersion: Int,
  applicationId: String,
  cobrandConversationCredentials: ConversationCredentials,
  preferenceInfo: PreferenceInfo
)
object UserContext {
  implicit val reads = Json.reads[UserContext]
}

case class UserType(userTypeId: Int, userTypeName: String)
object UserType {
  implicit val reads = Json.reads[UserType]
}

case object InvalidCobrandContextException extends YodleeLogin
case object InvalidUserCredentialsException extends YodleeLogin
case object UserUncertifiedException extends YodleeLogin
case object UserAccountLockedException extends YodleeLogin
case object UserUnregisteredException extends YodleeLogin
case object UserStateChangedException extends YodleeLogin
case object YodleeAttributeException extends YodleeLogin
case object UserSuspendedException extends YodleeLogin
case object MaxUserCountExceededException extends YodleeLogin
case object UserGroupNotFoundException extends YodleeLogin
case object IllegalArgumentValueException extends YodleeLogin
case class YodleeLoginException(msg: String) extends YodleeLogin
case class UserInfo(
  userContext: UserContext,
  lastLoginTime: Long,
  loginCount: Int,
  passwordRecovered: Boolean,
  emailAddress: String,
  loginName: String,
  userId: Int,
  userType: UserType
) extends YodleeLogin
object UserInfo {
  implicit val reads = Json.reads[UserInfo]
}

sealed trait YodleeServiceInfo  /* Step 3 */
case class AutoRegistration(paperBillSuppressionType: String)
object AutoRegistration {
  implicit val reads = Json.reads[AutoRegistration]
}

case class AutoPay(paperBillSuppressionType: String)
object AutoPay {
  implicit val reads = Json.reads[AutoPay]
}

case class DirectPay(paperBillSuppressionType: String)
object DirectPay {
  implicit val reads = Json.reads[DirectPay]
}

case class ContainerInfo(containerName: String, assetType: Int)
object ContainerInfo {
  implicit val reads = Json.reads[ContainerInfo]
}

case class Country(country: String)
object Country {
  implicit val reads = Json.reads[Country]
}

case object InvalidCobrandConversationCredentialsException extends YodleeServiceInfo
case object InvalidRoutingNumberException extends YodleeServiceInfo
case class YodleeServiceInfoException(msg: String) extends YodleeServiceInfo
/* should workk with sbt 1.0
case class ContentServiceInfo(
  contentServiceId: Int, serviceId: Int,
  contentServiceDisplayName: String, organizationId: Int,
  organizationDisplayName: String, siteId: Int, siteDisplayName: String,
  custom: Boolean, loginUrl: String, homeUrl: String, registrationUrl: String,
  contactUrl: String, containerInfo: ContainerInfo,
  isCredentialRequired: Boolean, autoRegistrationSupported: Boolean,
  autoLoginType: Int, geographicRegionsServed: Array[String],
  autoPayCardSetupSupported: Boolean, directCardPaymentSupported: Boolean,
  directCheckPaymentSupported: Boolean,
  autoPayCardCancelSupported: Boolean, paymentVerificationSupported: Boolean,
  supportedAutoPaySetupCardTypeIds: Array[Int],
  supportedDirectPaymentCardTypeIds: Array[Int],
  hasPaymentHistory: Boolean, timeToUpdatePaymentHistory: Int,
  timeToPostDirectCardPayment: Int, isCSCForDirectPaymRequired: Boolean,
  isCSCForAutoPayRequired: Boolean, timeZoneId: String,
  isIAVFastSupported: Boolean, hasSiblingContentServices: Boolean,
  isFTEnabled: Boolean, isOnlinePaymentSupported: Boolean,
  autoRegistrationPaperBillSuppressionType: AutoRegistration,
  autoPayCardPaperBillSuppressionType: AutoPay,
  directCardPaymentPaperBillSuppressionType: DirectPay,
  addItemAccountSupported: Boolean, isAddAccountMultiFormAction: Boolean,
  isAutoRegistrationMultiFormAction: Boolean,
  isAddItemAccountMultiFormAction: Boolean, isSiteCredentialsStored: Boolean,
  isPaymentAmountRequiredForAutopay: Boolean,
  isNumberOfPaymentsRequiredForAutopay: Boolean,
  isFrequencyRequiredForAutopay: Boolean,
  supportedAutopayFrequencyTypes: Array[String],
  isConveninceFeeChargedForDirectCardPayment: Boolean,
  conveninceFeeRuleMessage: String, isEBillPaymSupprtd: Boolean,
  isBetaSite: Boolean, isBPAASource: Boolean, isBPAADest: Boolean,
  supportedBPSRecurringFrequencies: Array[String],
  checkLeadInterval: Int, cardLeadInterval: Int,
  isDirectTransferSupported: Boolean, isSiblingAutoAdditionSafe: Boolean,
  defaultHelpText: String
) extends YodleeServiceInfo
*/
case class ContentServiceInfoSeq1(
  contentServiceId: Int, serviceId: Int,
  contentServiceDisplayName: String, organizationId: Int,
  organizationDisplayName: String, siteId: Int, siteDisplayName: String,
  custom: Boolean, loginUrl: String, homeUrl: String, registrationUrl: String,
  contactUrl: String, containerInfo: ContainerInfo,
  isCredentialRequired: Boolean, autoRegistrationSupported: Boolean,
  autoLoginType: Int, geographicRegionsServed: List[Country],
  autoPayCardSetupSupported: Boolean, directCardPaymentSupported: Boolean,
  directCheckPaymentSupported: Boolean
)
object ContentServiceInfoSeq1 {
  implicit val reads = Json.reads[ContentServiceInfoSeq1]
}

case class ContentServiceInfoSeq2(
  autoPayCardCancelSupported: Boolean, paymentVerificationSupported: Boolean,
  supportedAutoPaySetupCardTypeIds: List[Int],
  supportedDirectPaymentCardTypeIds: List[Int],
  hasPaymentHistory: Boolean, timeToUpdatePaymentHistory: Int,
  timeToPostDirectCardPayment: Int, isCSCForDirectPaymRequired: Boolean,
  isCSCForAutoPayRequired: Boolean, timeZoneId: String,
  isIAVFastSupported: Boolean, hasSiblingContentServices: Boolean,
  isFTEnabled: Boolean, isOnlinePaymentSupported: Boolean,
  autoRegistrationPaperBillSuppressionType: AutoRegistration,
  autoPayCardPaperBillSuppressionType: AutoPay,
  directCardPaymentPaperBillSuppressionType: DirectPay,
  addItemAccountSupported: Boolean, isAddAccountMultiFormAction: Boolean,
  isAutoRegistrationMultiFormAction: Boolean
)
object ContentServiceInfoSeq2 {
  implicit val reads = Json.reads[ContentServiceInfoSeq2]
}
case class ContentServiceInfoSeq3(
  isAddItemAccountMultiFormAction: Boolean, isSiteCredentialsStored: Boolean,
  isPaymentAmountRequiredForAutopay: Boolean,
  isNumberOfPaymentsRequiredForAutopay: Boolean,
  isFrequencyRequiredForAutopay: Boolean,
  supportedAutopayFrequencyTypes: List[String],
  isConveninceFeeChargedForDirectCardPayment: Boolean,
  conveninceFeeRuleMessage: String, isEBillPaymSupprtd: Boolean,
  isBetaSite: Boolean, isBPAASource: Boolean, isBPAADest: Boolean,
  supportedBPSRecurringFrequencies: List[String],
  checkLeadInterval: Int, cardLeadInterval: Int,
  isDirectTransferSupported: Boolean, isSiblingAutoAdditionSafe: Boolean,
  defaultHelpText: String
)
object ContentServiceInfoSeq3 {
  implicit val reads = Json.reads[ContentServiceInfoSeq3]
}

case class ContentServiceInfo(
  seq1: ContentServiceInfoSeq1,
  seq2: ContentServiceInfoSeq2,
  seq3: ContentServiceInfoSeq3
) extends YodleeServiceInfo {
  def contentServiceId: Int = seq1.contentServiceId
  def serviceId: Int = seq1.serviceId
  def contentServiceDisplayName: String = seq1.contentServiceDisplayName
  def organizationId: Int = seq1.organizationId
  def organizationDisplayName: String = seq1.organizationDisplayName
  def siteId: Int = seq1.siteId
  def siteDisplayName: String = seq1.siteDisplayName
  def custom: Boolean = seq1.custom
  def loginUrl: String = seq1.loginUrl
  def homeUrl: String = seq1.homeUrl
  def registrationUrl: String = seq1.registrationUrl
  def contactUrl: String = seq1.contactUrl
  def containerInfo: ContainerInfo = seq1.containerInfo
  def isCredentialRequired: Boolean = seq1.isCredentialRequired
  def autoRegistrationSupported: Boolean = seq1.autoRegistrationSupported
  def autoLoginType: Int = seq1.autoLoginType
  def geographicRegionsServed: List[Country] = seq1.geographicRegionsServed
  def autoPayCardSetupSupported: Boolean = seq1.autoPayCardSetupSupported
  def directCardPaymentSupported: Boolean = seq1.directCardPaymentSupported
  def directCheckPaymentSupported: Boolean = seq1.directCheckPaymentSupported
  def autoPayCardCancelSupported: Boolean = seq2.autoPayCardCancelSupported
  def paymentVerificationSupported: Boolean = seq2.paymentVerificationSupported
  def supportedAutoPaySetupCardTypeIds: List[Int] = seq2.supportedAutoPaySetupCardTypeIds
  def supportedDirectPaymentCardTypeIds: List[Int] = seq2.supportedDirectPaymentCardTypeIds
  def hasPaymentHistory: Boolean = seq2.hasPaymentHistory
  def timeToUpdatePaymentHistory: Int = seq2.timeToUpdatePaymentHistory
  def timeToPostDirectCardPayment: Int = seq2.timeToPostDirectCardPayment
  def isCSCForDirectPaymRequired: Boolean = seq2.isCSCForDirectPaymRequired
  def isCSCForAutoPayRequired: Boolean = seq2.isCSCForAutoPayRequired
  def timeZoneId: String = seq2.timeZoneId
  def isIAVFastSupported: Boolean = seq2.isIAVFastSupported
  def hasSiblingContentServices: Boolean = seq2.hasSiblingContentServices
  def isFTEnabled: Boolean = seq2.isFTEnabled
  def isOnlinePaymentSupported: Boolean = seq2.isOnlinePaymentSupported
  def autoRegistrationPaperBillSuppressionType: AutoRegistration = seq2.autoRegistrationPaperBillSuppressionType
  def autoPayCardPaperBillSuppressionType: AutoPay = seq2.autoPayCardPaperBillSuppressionType
  def directCardPaymentPaperBillSuppressionType: DirectPay = seq2.directCardPaymentPaperBillSuppressionType
  def addItemAccountSupported: Boolean = seq2.addItemAccountSupported
  def isAddAccountMultiFormAction: Boolean = seq2.isAddAccountMultiFormAction
  def isAutoRegistrationMultiFormAction: Boolean = seq2.isAutoRegistrationMultiFormAction
  def isAddItemAccountMultiFormAction: Boolean = seq3.isAddItemAccountMultiFormAction
  def isSiteCredentialsStored: Boolean = seq3.isSiteCredentialsStored
  def isPaymentAmountRequiredForAutopay: Boolean = seq3.isPaymentAmountRequiredForAutopay
  def isNumberOfPaymentsRequiredForAutopay: Boolean = seq3.isNumberOfPaymentsRequiredForAutopay
  def isFrequencyRequiredForAutopay: Boolean = seq3.isFrequencyRequiredForAutopay
  def supportedAutopayFrequencyTypes: List[String] = seq3.supportedAutopayFrequencyTypes
  def isConveninceFeeChargedForDirectCardPayment: Boolean = seq3.isConveninceFeeChargedForDirectCardPayment
  def conveninceFeeRuleMessage: String = seq3.conveninceFeeRuleMessage
  def isEBillPaymSupprtd: Boolean = seq3.isEBillPaymSupprtd
  def isBetaSite: Boolean = seq3.isBetaSite
  def isBPAASource: Boolean = seq3.isBPAASource
  def isBPAADest: Boolean = seq3.isBPAADest
  def supportedBPSRecurringFrequencies: List[String] = seq3.supportedBPSRecurringFrequencies
  def checkLeadInterval: Int = seq3.checkLeadInterval
  def cardLeadInterval: Int = seq3.cardLeadInterval
  def isDirectTransferSupported: Boolean = seq3.isDirectTransferSupported
  def isSiblingAutoAdditionSafe: Boolean = seq3.isSiblingAutoAdditionSafe
  def defaultHelpText: String = seq3.defaultHelpText
}

object ContentServiceInfo {
  implicit val reads = Json.reads[ContentServiceInfo]
}

/* Step 4 */
sealed trait YodleeLoginForm
case class YodleeLoginFormException(msg: String) extends YodleeLoginForm
//case object InvalidCobrandConversationCredentialsException extends YodleeLoginForm
//case object ContentServiceNotFoundException extends YodleeLoginForm
case class LoginForm(dummy: String) extends YodleeLoginForm

/* Step 5 */
sealed trait YodleeIAVRefreshStatus
case class FieldType(typeName: String) // TODO: Replace with ENUM
object FieldType {
  implicit val reads = Json.reads[FieldType]
}
case class RefreshStatus(status: Int) // TODO: replace Int with ENUM
object RefreshStatus {
  implicit val reads = Json.reads[RefreshStatus]
}
case class CredentialFields(
  displayName: String, fieldType: FieldType, helpText: Int, isEditable: Boolean,
  maxlength: Int, name: String, size: Int, value: String, valueIdentifier: String,
  valueMask: String)
object CredentialFields {
  implicit val reads = Json.reads[CredentialFields]
}

case class IAVRefreshStatus(
  refreshStatus: RefreshStatus, itemId: Int
) extends YodleeIAVRefreshStatus
object IAVRefreshStatus {
  implicit val reads = Json.reads[IAVRefreshStatus]
}
//case object InvalidCobrandConversationCredentialsException extends YodleeIAVRefreshStatus
//case object ContentServiceNotFoundException extends YodleeIAVRefreshStatus
//case object IllegalArgumentValueException extends YodleeIAVRefreshStatus
case object InvalidConversationCredentialsException extends YodleeIAVRefreshStatus
case object IncompleteArgumentException extends YodleeIAVRefreshStatus
case object IAVDataRequestNotSupportedException extends YodleeIAVRefreshStatus
case class YodleeIAVRefreshStatusException(msg: String) extends YodleeIAVRefreshStatus

/* Step 6a/b */
sealed trait FieldInfo

case class QAValues(
  question: String,
  questionFieldType: String,
  responseFieldType: String,
  isRequired: String,
  sequence: Int,
  metaData: String
)
object QAValues {
  implicit val reads = Json.reads[QAValues]
}
case class SecurityQuestionFieldInfo(
  questionAndAnswerValues: List[QAValues],
  numOfMandatoryQuestions: Int
) extends FieldInfo
object SecurityQuestionFieldInfo {
  implicit val reads = Json.reads[SecurityQuestionFieldInfo]
}

case class TokenFieldInfo(
  responseFieldType: String,
  minimumLength: Int,
  maximumLength: Int,
  displayString: String
) extends FieldInfo
object TokenFieldInfo {
  implicit val reads = Json.reads[TokenFieldInfo]
}

case class ImageFieldInfo(
  responseFieldType: String,
  imageFieldType: String,
  image: List[Int],
  minimumLength: Int,
  maximumLength: Int,
  displayString: String
) extends FieldInfo
object ImageFieldInfo {
  implicit val reads = Json.reads[ImageFieldInfo]
}

sealed trait YodleeMFA
//case object InvalidCobrandConversationCredentialsException extendsYodleeMFA
//case object InvalidConversationCredentialsException extendsYodleeMFA
case object InvalidItemException extends YodleeMFA
case object MFARefreshException extends YodleeMFA
case class YodleeMFAException(msg: String) extends YodleeMFA
case class MFARefreshInfoToken(
  isMessageAvailable: Boolean,
  fieldInfo: TokenFieldInfo,
  timeOutTime: Int,
  itemId: Int,
  retry: Boolean
) extends YodleeMFA
object MFARefreshInfoToken {
  implicit val reads = Json.reads[MFARefreshInfoToken]
}

case class MFARefreshInfoImage(
  isMessageAvailable: Boolean,
  fieldInfo: ImageFieldInfo,
  timeOutTime: Int,
  itemId: Int,
  retry: Boolean
) extends YodleeMFA
object MFARefreshInfoImage {
  implicit val reads = Json.reads[MFARefreshInfoImage]
}

case class MFARefreshInfoQuestion(
  isMessageAvailable: Boolean,
  fieldInfo: SecurityQuestionFieldInfo,
  timeOutTime: Int,
  itemId: Int,
  retry: Boolean
) extends YodleeMFA
object MFARefreshInfoQuestion {
  implicit val reads = Json.reads[MFARefreshInfoQuestion]
}

/* Step 6b */
case class MFAPutResponse(response: String) extends YodleeMFA
object MFAPutResponse {
  implicit val reads = Json.reads[MFAPutResponse]
}

//case object IllegalArgumentValueException extends YodleeMFA
case class MFAUserResponse(dummy: String)

/* Step 7 */
sealed trait YodleeVeriticationData
case class RequestStatus(verificationRequestStatus: String)
object RequestStatus {
  implicit val reads = Json.reads[RequestStatus]
}
case class RequestType(name: String)
object RequestType {
  implicit val reads = Json.reads[RequestType]
}
case class ItemInfo(
  transactionId: String,
  itemId: Int,
  contentServiceId: Int,
  requestStatus: RequestStatus,
  requestType: RequestType,
  requestTime: String,
  completionTime: String,
  intRespCompletionTime: String,
  completed: Boolean,
  statusCode: Int,
  requestedLocale: String,
  derivedLocale: String
)
object ItemInfo {
  implicit val reads = Json.reads[ItemInfo]
}
case class AccountType(name: String)
object AccountType {
  implicit val reads = Json.reads[AccountType]
}
case class Balance(amount: Int, currencyCode: String)
object Balance {
  implicit val reads = Json.reads[Balance]
}
case class AccountHolder(fullName: String)
object AccountHolder {
  implicit val reads = Json.reads[AccountHolder]
}
case class AccountData(
  accountType: AccountType,
  availableBalance: Balance,
  itemVerificationInfo: ItemInfo,
  accountNumber: String,
  accountName: String,
  accountHolder: AccountHolder
)
object AccountData {
  implicit val reads = Json.reads[AccountData]
}
case class VerificationItem(itemVerificationInfo: ItemInfo, accountVerificationData: AccountData)
object VerificationItem {
  implicit val reads = Json.reads[VerificationItem]
}
case class ItemVerificationData(items: List[VerificationItem]) extends YodleeVeriticationData
object ItemVerificationData {
  implicit val reads = Json.reads[ItemVerificationData]
}

case class YodleeVeriticationDataException(msg: String) extends YodleeVeriticationData
/*
case object InvalidCobrandConversationCredentialsException
case object InvalidConversationCredentialsException
case object InvalidItemException
case object IllegalArgumentValueException
*/

/* ========================= */
/* YodleeWS singleton object */
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
  implicit val client: WSClient = {
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

  def loginConsumer(cobToken: String, userLogin: String, userPass: String):
    Future[YodleeLogin] = {

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

  def getContentServiceInfo(cobToken: String, routingNumber: Int, notrim: Boolean):
    Future[YodleeServiceInfo] = {

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
      val arg1 = json.validate[ContentServiceInfoSeq1].fold(invalidHandler(json), identity)
      val arg2 = json.validate[ContentServiceInfoSeq2].fold(invalidHandler(json), identity)
      val arg3 = json.validate[ContentServiceInfoSeq3].fold(invalidHandler(json), identity)
      (arg1, arg2, arg3) match {
        case (ex: YodleeServiceInfo, _, _) => ex
        case (_, ex: YodleeServiceInfo, _) => ex
        case (_, _, ex: YodleeServiceInfo) => ex
        case _ => ContentServiceInfo(arg1.asInstanceOf[ContentServiceInfoSeq1],
                                     arg2.asInstanceOf[ContentServiceInfoSeq2],
                                     arg3.asInstanceOf[ContentServiceInfoSeq3])
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
      LoginForm("unimplemented yet")
    }

    async {
      implicit val timeout: FiniteDuration = 4000 millis
      val resp = await {
        request.post(data) withTimeout timeoutEx("loginForm request timeout")
      }
      validateResponse(resp.json)
    }

  }

  def addItemAndStartVerification(cobToken: String, userSessionToken: String,
    contentServiceId: Int, accountNumber: Int, routingNumber: Int,
    credentialsEnclosedType: String, credentialFields: (CredentialFields, CredentialFields)):
    Future[YodleeIAVRefreshStatus] = {

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

  def getMFAResponse(cobToken: String, userSessionToken: String, itemId: Int):
    Future[YodleeMFA] = {
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

  def registerNewConsumer = ???

  /* TODOs:
   * 1. Factor out all Exceptions into YodleeException trait
   * 2. Introduce YodleeCommonException(msg: String) to handle unknown cases
   * 3. Return Future[Either[YodleeException, YodleeSomeResponse]] => scalaz
   * 4. Decide what to do with LoginForms
   * 5. Introduce functions to convert input objects into data for POST req
   * 6. Factor out clients into common mkClient function
   * 7. Factor our async/await blocks
   * 8. Factor out client into unit tests and general client test that goes
   * through the whole verification cycle
   */

  def shutdown(by: String): Unit = {
    log.info(s"shutdown by $by")
    client.close()
    system.shutdown
  }

  /* Client Code */
  def start: Unit = {
    /* step 1 */
    val cobToken = authenticateStep

    for (exc <- cobToken.failed) {
      log.info(exc.getMessage)
      shutdown("Step1")
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
