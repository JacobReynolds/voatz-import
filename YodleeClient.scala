package org.nimsim.voatz.yodlee

import scala.language.postfixOps
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Try, Success, Failure }
import scala.annotation.tailrec

object Client extends App {
  import YodleeWS._

  /* Client Code */
  def start: Unit = {

    /* step 1 */
    val cobToken = {
      val auth = authenticateStep
      for (exc <- auth.failed) {
        log.info("Step 1 Exception: " + exc.getMessage)
        shutdown("Step1")
      }
      auth
    }

    /* step 2b */
    val userToken = cobToken flatMap { t =>
      val userSessionToken = loginStep(t)
      for (exc <- userSessionToken.failed) {
        log.info("Step 2b Exception: " + exc.getMessage)
        shutdown("Step 2b")
      }
      userSessionToken
    }

    /* step3 */
    val serviceID = cobToken flatMap { t =>
      val contentServiceId = serviceInfoStep(t)
      for (exc <- contentServiceId.failed) {
        log.info("Step 3 Exception: " + exc.getMessage)
        shutdown("Step 3")
      }
      contentServiceId
    }

    val loginFormVal: Future[String] = {
      val form = (for {
        token: String <- cobToken
        id: Int <- serviceID
      } yield loginFormStep(token, id)) flatMap(identity)

      for (exc <- form.failed) {
        log.info("Step 4 Exception: " + exc.getMessage)
        shutdown("Step 4")
      }
      form
    }

    val refreshTuple = {
      val tuple = (for {
        token <- cobToken
        userToken <- userToken
        id <- serviceID
      } yield startVerificationStep(token, userToken, id)) flatMap(identity)

      for (exc <- tuple.failed) {
        log.info("Step 5 Exception: " + exc.getMessage)
        shutdown("Step 5")
      }
      tuple
    }

    def getMFAInfo: Future[Boolean] = {
      log.info("getMFAInfo call")
      val infoObj = (for {
        token <- cobToken
        userToken <- userToken
        (st, id) <- refreshTuple
      } yield getMFAStep(token, userToken, id)) flatMap(identity)

      for (exc <- infoObj.failed) {
        log.info("Step 6 Exception: " + exc.getMessage)
        shutdown("Step 6")
      }

      infoObj
    }

    def putMFARequest: Future[Boolean] = {
      val value = (for {
        token <- cobToken
        userToken <- userToken
        (st, id) <- refreshTuple
      } yield putMFAStep(token, userToken, id)) flatMap(identity)

      for (exc <- value.failed) {
        log.info("Step 6b Exception: " + exc.getMessage)
        shutdown("Step 6b")
      }
      value
    }

    def callMFA: Future[Boolean] = {
      refreshTuple flatMap { case(st, id) =>
        if (st == 8) {
          getMFAInfo flatMap { st =>
            if (st == true) callMFA
            else putMFARequest map { r => log.info(s"put status: $r"); r }
          }
        } else Future { true }
      }
    }

    def verifyData: Future[Int] = {
      val status = (for {
        token <- cobToken
        userToken <- userToken
        (st, id) <- refreshTuple
      } yield finalStep(token, userToken, id)) flatMap(identity)

      for (exc <- status.failed) {
        log.info("Step 7 Exception: " + exc.getMessage)
        shutdown("Step 7")
      }
      status
    }

    def loopVerfifyData: Future[Int] = {
      verifyData flatMap { st =>
        if (st == 801) loopVerfifyData
        else Future { st }
      }
    }

    cobToken foreach { tn => log.info(s"cobToken = $tn") }
    userToken foreach { ut => log.info(s"userToken = $ut") }
    serviceID foreach { id => log.info(s"contentServiceId = $id") }
    loginFormVal foreach { v => log.info(s"form = $v") }
    refreshTuple foreach { case(st, id) => log.info(s"$st and $id") }
    loopVerfifyData map { st => log.info(s"final status = $st"); st } andThen { case _ => shutdown("Step Final") }
    //callMFA foreach { st => log.info(s"mfa refresh status: $st") }
  }

  def authenticateStep: Future[String] = {
    val voatzLogin = "sbCobvulcan"
    val voatzPass = "e50e4dfa-407e-4713-9008-640be6f78fea"
    val authRes = authCobrand(AuthenticateInput(voatzLogin, voatzPass))

    /* get sessionToken or the exception */
    authRes map {
      _ match {
        case Right(r: AuthenticateCobrand) => r.cobrandConversationCredentials.sessionToken
        case Left(e: YodleeSimpleException) => throw new Exception(e.message)
      }
    }
  }

  def loginStep(token: String): Future[String] = {
    val username = "sbMemvulcan5"
    val password = "sbMemvulcan5#123"
    val loginResp = loginConsumer(LoginInput(token, username, password))

    loginResp map { _ match {
        case Right(r: UserInfo) => r.userContext.conversationCredentials.sessionToken
        case Left(e: YodleeSimpleException) => throw new Exception(e.message)
      }
    }
  }

  def serviceInfoStep(token: String): Future[Int] = {
    val dagRoutingNumber = 999999989
    //val dagRoutingNumber = 910080000
    val serviceInfo =
      getContentServiceInfo(ContentServiceInfoInput(token, dagRoutingNumber, true))

    serviceInfo map { _ match {
        case Right(r: ContentServiceInfo) => r.seq1.contentServiceId
        case Left(e: YodleeExtendedException) => throw new Exception(e.message)
      }
    }
  }

  def loginFormStep(token: String, id: Int): Future[String] = {
    val form = getLoginForm(LoginFormInput(token, id))
    form map { _ match {
        case Right(r: LoginForm) => r.defaultHelpText
        case Left(e: YodleeExtendedException) => throw new Exception(e.message)
      }
    }
  }

  def startVerificationStep(token: String,
                            userToken: String,
                            serviceId: Int): Future[(Int, Int)] = {
    val refreshStatus = startVerification(
      StartVerificationInput(token, userToken, serviceId, None,
        None, "com.yodlee.common.FieldInfoSingle", List(
        CredentialFields("USLoginId", FieldType("TEXT"), 22059, true, 40,
          "LOGIN", 20, "vulcan.bank2", "LOGIN", "LOGIN_FIELD"),

        /* Simple DAG
        CredentialFields("Password", FieldType("IF_PASSWORD"), 92429, true,
          40, "PASSWORD", 20, "bank1", "PASSWORD", "LOGIN_FIELD"))))
        */
        CredentialFields("Password", FieldType("IF_PASSWORD"), 22058, true,
          40, "PASSWORD1", 20, "bank2", "PASSWORD1", "LOGIN_FIELD"))))
    refreshStatus map { _ match {
        case Right(r: IAVRefreshStatus) => (r.refreshStatus.status, r.itemId)
        case Left(e: YodleeExtendedException) => throw new Exception(e.message)
      }
    }
  }

  def getMFAStep(token: String, userToken: String, itemId: Int): Future[Boolean] = {
    val refreshInfo = getMFAResponse(GetMFAInput(token, userToken, itemId))
    refreshInfo map { _ match {
        case Right(r: MFARefreshInfoToken) => log.info("TokenMFA: " + r);r.retry
        case Right(r: MFARefreshInfoImage) => log.info("ImageMFA" + r); r.retry
        case Right(r: MFARefreshInfoQuestion) => log.info("QAMFA " + r); r.retry
        case Left(e: YodleeExtendedException) => throw new Exception(e.message)
      }
    }
  }

  def putMFAStep(token: String, userToken: String, itemId: Int): Future[Boolean] = {
    val value = putMFAResponse(PutMFAInput(
        GetMFAInput(token, userToken, itemId),
        MFAQAResponse("com.yodlee.core.mfarefresh.MFAQuesAnsResponse", Nil))
    )

    value map { _ match {
        case Right(r) => r.response toBoolean
        case Left(e: YodleeExtendedException) => throw new Exception(e.message)
      }
    }
  }

  def finalStep(token: String, userToken: String, itemId: Int) : Future[Int] = {
    val verificationData =
      getVerificationData(VerificationDataInput(token, userToken, List(itemId)))

    verificationData map { _ match {
        case Right(r) => log.info(r toString); r head match {
          case item: VerificationItem => item.itemVerificationInfo.statusCode
          case item: VerificationItemInProgress => item.itemVerificationInfo.statusCode
        }
        case Left(e: YodleeExtendedException) => throw new Exception(e.message)
      }
    }
  }

  start
}
