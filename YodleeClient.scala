package org.nimsim.voatz.yodlee

import scala.language.postfixOps
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Try, Success, Failure }
import scala.annotation.tailrec
import scala.async.Async.{async, await}

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

    def getMFAInfo: Future[(Boolean, Option[Int])] = {
      log.info("getMFAInfo called")
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

    def putMFAReq: Future[Boolean] = {
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
      def loopMFA: Future[Boolean] = { async {
        val (retry, code) = await { getMFAInfo }
        if (retry == false) { // no more refresh, proceed
          code match {
            case None => {   // put user response
              val put = await { putMFAReq }
              log.info(s"put status: ${ put }")
              await { loopMFA }
            }
            case Some(n) => log.info(s"errorCode = $n"); retry // done
          }
        } else await { loopMFA }  // refresh more
      } }

      async {
        val (st, id) = await { refreshTuple }
        if (st == 8) await { loopMFA } else true
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
    //loginFormVal foreach { v => log.info(s"form = $v") }
    refreshTuple foreach { case(st, id) => log.info(s"$st and $id") }
    callMFA foreach { st =>
      log.info(s"mfa refresh status: $st")
      loopVerfifyData map { st =>
        log.info(s"final status = $st"); st
      } andThen { case _ => shutdown("Step Final") }
    }
  }

  def authenticateStep: Future[String] = {
    val voatzLogin = "sbCobvulcan"
    val voatzPass = "e50e4dfa-407e-4713-9008-640be6f78fea"
    val authRes = authCobrand(AuthenticateInput(voatzLogin, voatzPass))

    /* get sessionToken or the exception */
    authRes map {
      _ match {
        case Right(r: AuthenticateCobrand) => r.cobrandConversationCredentials.sessionToken
        case Left(e: YodleeException) => throw new Exception(e toString)
      }
    }
  }

  def loginStep(token: String): Future[String] = {
    val username = "sbMemvulcan5"
    val password = "sbMemvulcan5#123"
    val loginResp = loginConsumer(LoginInput(token, username, password))

    loginResp map { _ match {
        case Right(r: UserInfo) => r.userContext.conversationCredentials.sessionToken
        case Left(e: YodleeException) => throw new Exception(e toString)
      }
    }
  }

  def serviceInfoStep(token: String): Future[Int] = {
    val dagRoutingNumber = 910080000
    val serviceInfo =
      getContentServiceInfo(ContentServiceInfoInput(token, dagRoutingNumber, true))

    serviceInfo map { _ match {
        case Right(r: ContentServiceInfo) => r.seq1.contentServiceId
        case Left(e: YodleeException) => throw new Exception(e toString)
      }
    }
  }

  /* really optional since it's passed in serviceInfoStep */
  def loginFormStep(token: String, id: Int): Future[String] = {
    val form = getLoginForm(LoginFormInput(token, id))
    form map { _ match {
        case Right(r: LoginForm) => r.defaultHelpText
        case Left(e: YodleeException) => throw new Exception(e toString)
      }
    }
  }

  def startVerificationStep(token: String,
                            userToken: String,
                            serviceId: Int): Future[(Int, Int)] = {
    val refreshStatus = startVerification(
      StartVerificationInput(
        token, userToken, "com.yodlee.common.FieldInfoSingle", List(
        CredentialFields("User Name", FieldType("TEXT"), 92430, true, 40,
          "LOGIN", 20, "vulcan.BankTokenFMPA1", "LOGIN", "LOGIN_FIELD"),
        CredentialFields("Password", FieldType("IF_PASSWORD"), 92429, true,
          40, "PASSWORD", 20, "BankTokenFMPA1", "PASSWORD", "LOGIN_FIELD")),
        IAVRequest(serviceId)))
    refreshStatus map { _ match {
        case Right(r: IAVRefreshStatus) => (r.refreshStatus.status, r.itemId)
        case Left(e: YodleeException) => throw new Exception(e toString)
      }
    }
  }

  def getMFAStep(token: String, userToken: String, itemId: Int):
  Future[(Boolean, Option[Int])] = {
    val refreshInfo = getMFAResponse(GetMFAInput(token, userToken, itemId))
    refreshInfo map { _ match {
        case Right(r: MFARefreshInfoToken) => r.retry -> None
        case Right(r: MFARefreshInfoImage) => r.retry -> None
        case Right(r: MFARefreshInfoQuestion) => r.retry -> None
        case Right(r: MFARefreshDone) => r.retry -> Some(r.errorCode)
        case Left(e: YodleeException) => throw new Exception(e toString)
      }
    }
  }

  def putMFAStep(token: String, userToken: String, itemId: Int): Future[Boolean] = {
    val value = putMFARequest(PutMFAInput(
        GetMFAInput(token, userToken, itemId),
        MFATokenResponse("com.yodlee.core.mfarefresh.MFATokenResponse", "123456")
      )
    )

    value map { _ match {
        case Right(r: MFAPutResponse) => r.primitiveObj
        case Left(e: YodleeException) => throw new Exception(e toString)
      }
    }
  }

  def finalStep(token: String, userToken: String, itemId: Int) : Future[Int] = {
    val verificationData =
      getVerificationData(VerificationDataInput(token, userToken, List(itemId)))

    verificationData map { _ match {
        case Right(r) => r head match {
          case item: VerificationItem => item.itemVerificationInfo.statusCode
          case item: VerificationItemInProgress => item.itemVerificationInfo.statusCode
        }
        case Left(e: YodleeException) => throw new Exception(e toString)
      }
    }
  }

  start
}
