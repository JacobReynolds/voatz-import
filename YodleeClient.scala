package org.nimsim.voatz.yodlee

import scala.language.postfixOps
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Client extends App {
  import YodleeWS._

  /* Client Code */
  def start: Unit = {

    /* step 1 */
    def cobToken = {
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
    def serviceID = cobToken flatMap { t =>
      val contentServiceId = serviceInfoStep(t)
      for (exc <- contentServiceId.failed) {
        log.info("Step 3 Exception: " + exc.getMessage)
        shutdown("Step 3")
      }
      contentServiceId
    }

    def loginFormVal: Future[String] = {
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

    def refreshTuple = {
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

    def getMFA: Future[Boolean] = {
      val infoObj = (for {
        token <- cobToken
        userToken <- userToken
        (st, id) <- refreshTuple
        if st == 8
      } yield getMFAStep(token, userToken, id)) flatMap(identity)

      for {
        retry <- infoObj
        if retry == true
      } yield getMFA

      for (exc <- infoObj.failed) {
        log.info("Step 6 Exception: " + exc.getMessage)
        shutdown("Step 6")
      }
      infoObj
    }

    cobToken foreach { tn => log.info(s"cobToken = $tn") }
    userToken foreach { ut => log.info(s"userToken = $ut") }
    serviceID foreach { id => log.info(s"contentServiceId = $id") }
    loginFormVal foreach { v => log.info(s"form = $v") }
    refreshTuple foreach { case(st, id) => log.info(s"$st and $id") }
    getMFA map { r => log.info(s"$r") } andThen { case _ => shutdown("Step Final") }

  }

  def authenticateStep: Future[String] = {
    val voatzLogin = "sbCobvulcan"
    val voatzPass = "e50e4dfa-407e-4713-9008-640be6f78fea"
    val authRes = authCobrand(AuthenticateInput(voatzLogin, voatzPass))

    /* get sessionToken or the exception */
    authRes map { _ match {
      case Right(r: AuthenticateCobrand) => r.cobrandConversationCredentials.sessionToken
      case Left(e) => throw new Exception(e.message)
      }
    }
  }

  def loginStep(token: String): Future[String] = {
    val username = "sbMemvulcan2"
    val password = "sbMemvulcan2#123"
    val loginResp = loginConsumer(LoginInput(token, username, password))

    loginResp map { _ match {
        case Right(r: UserInfo) => r.userContext.conversationCredentials.sessionToken
        case Left(e) => throw new Exception(e.message)
      }
    }
  }

  def serviceInfoStep(token: String): Future[Int] = {
    val dagRoutingNumber = 910080000
    val serviceInfo =
      getContentServiceInfo(ContentServiceInfoInput(token, dagRoutingNumber, true))

    serviceInfo map { _ match {
        case Right(r: ContentServiceInfo) => r.seq1.contentServiceId
        case Left(e) => throw new Exception(e.message)
      }
    }
  }

  def loginFormStep(token: String, id: Int): Future[String] = {
    val form = getLoginForm(LoginFormInput(token, id))
    form map { _ match {
        case Right(r: LoginForm) => r.defaultHelpText
        case Left(e) => throw new Exception(e.message)
      }
    }
  }

  def startVerificationStep(token: String,
                            userToken: String,
                            serviceId: Int): Future[(Int, Int)] = {
    val refreshStatus = startVerification(
      StartVerificationInput(token, userToken, serviceId, Some(503-1123001),
        Some(910080000), "com.yodlee.common.FieldInfoSingle", List(
        CredentialFields("USLoginId", FieldType("TEXT"), 22059, true, 40,
          "LOGIN", 20, "dataservice.bank1", "LOGIN", "LOGIN_FIELD"),

        CredentialFields("Password", FieldType("IF_PASSWORD"), 92429, true,
          40, "PASSWORD", 20, "bank1", "PASSWORD", "LOGIN_FIELD"))))
        /* Simple DAG
        CredentialFields("USPassword", FieldType("IF_PASSWORD"), 22058, true,
          40, "PASSWORD1", 20, "bank1", "PASSWORD1", "LOGIN_FIELD"))))
        */
    refreshStatus map { _ match {
        case Right(r: IAVRefreshStatus) => (r.refreshStatus.status, r.itemId)
        case Left(e) => throw new Exception(e.message)
      }
    }
  }

  def getMFAStep(token: String, userToken: String, itemId: Int): Future[Boolean] = {
    val refreshInfo = getMFAResponse(GetMFAInput(token, userToken, itemId))
    refreshInfo map { _ match {
        case Right(r: MFARefreshInfoToken) => log.info("TokenMFA: " + r); r.retry
        case Right(r: MFARefreshInfoImage) => log.info("ImageMFA" + r); r.retry
        case Right(r: MFARefreshInfoQuestion) => log.info("QAMFA" + r); r.retry
        case Left(e) => throw new Exception(e.message)
      }
    }
  }

  start
}
