package org.nimsim.voatz.yodlee

import play.api.libs.json._

sealed trait YodleePost /* inputs to all methods */
sealed trait Component
sealed trait FieldInfo
sealed trait MFAUserResponse
sealed trait UserResponse

sealed trait YodleeException
sealed trait YodleeResponse
sealed trait YodleeAuthenticate extends YodleeResponse
sealed trait YodleeRegister extends YodleeResponse
sealed trait YodleeLogin extends YodleeResponse
sealed trait YodleeServiceInfo extends YodleeResponse
sealed trait YodleeLoginForm extends YodleeResponse
sealed trait YodleeIAVRefreshStatus extends YodleeResponse
sealed trait YodleeMFA extends YodleeResponse
sealed trait YodleeVerificationData extends YodleeResponse

case class AuthenticateInput(
  cobrandLogin: String,
  cobrandPassword: String
) extends YodleePost
case class LoginInput(
  cobSessionToken: String,
  login: String,
  password: String
) extends YodleePost
case class RegisterInput(
  cobSessionToken: String,
  userCredentials: UserCredentials,
  userProfile: UserProfile,
  userPreferences: List[(String, String)]
) extends YodleePost
case class ContentServiceInfoInput(
  cobSessionToken: String,
  routingNumber: Int,
  notrim: Boolean
) extends YodleePost
case class LoginFormInput(
  cobSessionToken: String,
  contentServiceId: Int
) extends YodleePost
case class StartVerificationInput(
  cobSessionToken: String,
  userSessionToken: String,
  contentServiceId: Int,
  accountNumber: Option[Int],
  routingNumber: Option[Int],
  enclosedType: String,
  credentialFields: List[CredentialFields]
) extends YodleePost
case class GetMFAInput(
  cobSessionToken: String,
  userSessionToken: String,
  itemId: Int
) extends YodleePost
case class PutMFAInput(
  getInput: GetMFAInput,
  userResponse: UserResponse
) extends YodleePost
case class VerificationDataInput(
  cobSessionToken: String,
  userSessionToken: String,
  itemIds: List[Int]
)

object MFAInstanceType extends Enumeration {
  type MFAInstanceType = Value
  val MFAToken = Value("com.yodlee.core.mfarefresh.MFATokenResponse")
  val MFAQA = Value("com.yodlee.core.mfarefresh.MFAQuesAnsResponse")
  val MFAImage = Value("com.yodlee.core.mfarefresh.MFAImageResponse")
}
import MFAInstanceType._

case class QA(
  answer: String,
  answerFieldType: String,
  metaData: String,
  question: String,
  questionFieldType: String
)
case class MFAQAResponse(
  objectInstanceType: String,
  quesAnsDetailArray: List[QA]
) extends UserResponse
case class MFAImageResponse(
  objectInstanceType: String,
  imageString: String
) extends UserResponse
case class MFATokenResponse(
  objectInstanceType: String,
  token: String
) extends UserResponse

case class UserCredentials(
  loginName: String,
  password: String,
  objectInstanceType: String
)
case class UserProfile(
  emailAddress: String,
  firstName: Option[String],
  lastName: Option[String],
  middleInitial: Option[String],
  objectInstanceType: Option[String],
  address1: Option[String],
  address2: Option[String],
  city: Option[String],
  country: Option[String]
)

case class FieldType (
  typeName: String
)
object FieldType {
  implicit val reads = Json.reads[FieldType]
}

case class CredentialFields(
  displayName: String,
  fieldType: FieldType,
  helpText: Int,
  isEditable: Boolean,
  maxlength: Int,
  name: String,
  size: Int,
  value: String,
  valueIdentifier: String,
  valueMask: String
)

/* all Exceptions */
case class YodleeExtendedException(
  errorOccurred: String,
  exceptionType: String,
  referenceCode: String,
  message: String
) extends YodleeException
object YodleeExtendedException {
  implicit val extendedReads = Json.reads[YodleeExtendedException]
}

case class YodleeSimpleException(message: String) extends YodleeException
object YodleeSimpleException {
  implicit val simpleReads = Json.reads[YodleeSimpleException]
}

object YodleeException {
  import YodleeSimpleException._
  import YodleeExtendedException._
  implicit val reads = simpleReads.map(identity[YodleeException]) orElse
                       extendedReads.map(identity[YodleeException])
}

/*
case object InvalidCobrandCredentialsException extends YodleeException
case object CobrandUserAccountLockedException extends YodleeException
case object InvalidCobrandContextException extends YodleeException
case object InvalidUserCredentialsException extends YodleeException
case object UserUncertifiedException extends YodleeException
case object UserAccountLockedException extends YodleeException
case object UserUnregisteredException extends YodleeException
case object UserStateChangedException extends YodleeException
case object YodleeAttributeException extends YodleeException
case object UserSuspendedException extends YodleeException
case object MaxUserCountExceededException extends YodleeException
case object UserGroupNotFoundException extends YodleeException
case object IllegalArgumentValueException extends YodleeException
case object IllegalArgumentTypeException extends YodleeException
case object UserNameExistsException extends YodleeException
case object InvalidCobrandConversationCredentialsException extends YodleeException
case object InvalidRoutingNumberException extends YodleeException
case object InvalidConversationCredentialsException extends YodleeException
case object IncompleteArgumentException extends YodleeException
case object IAVDataRequestNotSupportedException extends YodleeException
case object InvalidItemException extends YodleeException
case object MFARefreshException extends YodleeException
case object ContentServiceNotFoundException extends YodleeException
*/

/* Response Models for all steps */
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

case class ConversationCredentials(sessionToken: String)
object ConversationCredentials {
  implicit val reads = Json.reads[ConversationCredentials]
}


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

case class UserRegisterInfo(
  userContext: UserContext,
  lastLoginTime: Long,
  loginCount: Int,
  passwordRecovered: Boolean,
  emailAddress: String,
  loginName: String,
  userId: Int
) extends YodleeRegister
object UserRegisterInfo {
  implicit val reads = Json.reads[UserRegisterInfo]
}

case class UserType(userTypeId: Int, userTypeName: String)
object UserType {
  implicit val reads = Json.reads[UserType]
}

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

/* should work with sbt 1.0 when it's released
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
case class ConjunctionOp(
  conjuctionOp: Int
)
object ConjunctionOp {
  implicit val reads = Json.reads[ConjunctionOp]
}
case class SimpleComponent(
  valueIdentifier: String,
  valueMask: String,
  fieldType: FieldType,
  size: Int,
  maxlength: Int,
  name: String,
  displayName: String,
  isEditable: Boolean,
  isOptional: Boolean,
  isEscaped: Boolean,
  helpText: String,
  isOptionalMFA: Boolean,
  isMFA: Boolean
) extends Component
object SimpleComponent{
  implicit val simpleReads = Json.reads[SimpleComponent]
}

case class FieldComponent(
  defaultValues: List[String],
  values: List[String],
  validValues: List[String],
  displayValidValues: List[String],
  valueIdentifiers: List[String],
  valueMasks: List[String],
  fieldTypes: List[FieldType],
  validationRules: List[String],
  sizes: List[Int],
  maxlengths: List[Int],
  userProfileMappingExpressions: List[String],
  name: String,
  displayName: String,
  isEditable: Boolean,
  isOptional: Boolean,
  isEscaped: Boolean,
  helpText: String,
  isOptionalMFA: Boolean,
  isMFA: Boolean
) extends Component
object FieldComponent {
  implicit val fieldReads = Json.reads[FieldComponent]
}
case class DropDownComponent(
  validValues: List[String],
  displayValidValues: List[String],
  valueIdentifier: String,
  valueMask: String,
  fieldType: FieldType,
  size: Int,
  maxlength: Int,
  name: String,
  displayName: String,
  isEditable: Boolean,
  isOptional: Boolean,
  isEscaped: Boolean,
  helpText: String,
  isOptionalMFA: Boolean,
  isMFA: Boolean
) extends Component
object DropDownComponent {
  implicit val dropDownReads = Json.reads[DropDownComponent]
}
object Component {
  import FieldInfoListComponent._
  import DropDownComponent._
  import SimpleComponent._
  import FieldComponent._
  implicit val reads = simpleReads.map(identity[Component]) orElse
                       fieldReads.map(identity[Component]) orElse
                       dropDownReads.map(identity[Component]) orElse
                       infoListReads.map(identity[Component])
}

case class FieldInfoListComponent(fieldInfoList: List[Component]) extends Component
object FieldInfoListComponent {
  implicit val infoListReads: Reads[FieldInfoListComponent] = Json.reads[FieldInfoListComponent]
}

case class LoginForm(
  conjunctionOp: ConjunctionOp,
  componentList: List[Component],
  defaultHelpText: String
) extends YodleeLoginForm
object LoginForm {
  implicit val reads = Json.reads[LoginForm]
}

/* Step 5 */
case class RefreshStatus(status: Int) // TODO: replace Int with ENUM
object RefreshStatus {
  implicit val reads = Json.reads[RefreshStatus]
}

case class IAVRefreshStatus(
  refreshStatus: RefreshStatus,
  itemId: Int
) extends YodleeIAVRefreshStatus
object IAVRefreshStatus {
  implicit val reads = Json.reads[IAVRefreshStatus]
}

/* Step 6a/b */
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
case class MFARefreshInfoToken(
  isMessageAvailable: Boolean,
  fieldInfo: TokenFieldInfo,
  timeOutTime: Int,
  itemId: Int,
  retry: Boolean
) extends YodleeMFA
object MFARefreshInfoToken {
  implicit val tokenReads = Json.reads[MFARefreshInfoToken]
}

case class MFARefreshInfoImage(
  isMessageAvailable: Boolean,
  fieldInfo: ImageFieldInfo,
  timeOutTime: Int,
  itemId: Int,
  retry: Boolean
) extends YodleeMFA
object MFARefreshInfoImage {
  implicit val imageReads = Json.reads[MFARefreshInfoImage]
}

case class MFARefreshInfoQuestion(
  isMessageAvailable: Boolean,
  fieldInfo: SecurityQuestionFieldInfo,
  timeOutTime: Int,
  itemId: Int,
  retry: Boolean
) extends YodleeMFA
object MFARefreshInfoQuestion {
  implicit val qaReads = Json.reads[MFARefreshInfoQuestion]
}

case class MFARefreshDone(
  isMessageAvailable: Boolean,
  fieldInfo: SecurityQuestionFieldInfo,
  timeOutTime: Int,
  itemId: Int,
  errorCode: Int,
  retry: Boolean
) extends YodleeMFA
object MFARefreshDone {
  implicit val doneReads = Json.reads[MFARefreshDone]
}

object YodleeMFA {
  import MFARefreshInfoToken._
  import MFARefreshInfoImage._
  import MFARefreshInfoQuestion._
  import MFARefreshDone._

  implicit val reads = tokenReads.map(identity[YodleeMFA]) orElse
                       imageReads.map(identity[YodleeMFA]) orElse
                       qaReads.map(identity[YodleeMFA]) orElse
                       doneReads.map(identity[YodleeMFA])
}

/* Step 6b */
case class YodleeMFAPutResponse(response: String)
/*
object YodleeMFAPutResponse {
  implicit val reads = Json.reads[YodleeMFAPutResponse]
}
*/
/* Step 7 */
case class RequestStatus(
  verificationRequestStatus: String
)
object RequestStatus {
  implicit val reads = Json.reads[RequestStatus]
}
case class RequestType(
  name: String
)
object RequestType {
  implicit val reads = Json.reads[RequestType]
}
case class SiteRefreshStatus(
  siteRefreshStatusId: Int,
  siteRefreshStatus: String
)
object SiteRefreshStatus {
  implicit val reads = Json.reads[SiteRefreshStatus]
}
case class ItemVerificationInfo(
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
object ItemVerificationInfo {
  implicit val reads = Json.reads[ItemVerificationInfo]
}
case class ItemVerificationInfoInProgress(
  transactionId: String,
  itemId: Int,
  contentServiceId: Int,
  requestStatus: RequestStatus,
  requestType: RequestType,
  requestTime: String,
  completed: Boolean,
  statusCode: Int,
  requestedLocale: String,
  derivedLocale: String,
  siteRefreshStatus: SiteRefreshStatus
)
object ItemVerificationInfoInProgress {
  implicit val reads = Json.reads[ItemVerificationInfoInProgress]
}

case class AvailableBalance(
  amount: Int,
  currencyCode: String
)
object AvailableBalance {
  implicit val reads = Json.reads[AvailableBalance]
}
case class AccountHolder(
  fullName: String
)
object AccountHolder {
  implicit val reads = Json.reads[AccountHolder]
}
case class AccountVerificationData(
  accountType: RequestType,
  availableBalance: AvailableBalance,
  itemVerificationInfo: ItemVerificationInfo,
  accountNumber: String,
  accountName: String,
  accountHolder: AccountHolder
)
object AccountVerificationData {
  implicit val reads = Json.reads[AccountVerificationData]
}
case class VerificationItem(
  itemVerificationInfo: ItemVerificationInfo,
  accountVerificationData: List[AccountVerificationData]
) extends YodleeVerificationData
object VerificationItem {
  implicit val finalReads = Json.reads[VerificationItem]
}
case class VerificationItemInProgress(
  itemVerificationInfo: ItemVerificationInfoInProgress
) extends YodleeVerificationData
object VerificationItemInProgress {
  implicit val inProgressReads = Json.reads[VerificationItemInProgress]
}

object YodleeVerificationData {
  import VerificationItem._
  import VerificationItemInProgress._
  implicit val reads = finalReads.map(identity[YodleeVerificationData]) orElse
                       inProgressReads.map(identity[YodleeVerificationData])
}
