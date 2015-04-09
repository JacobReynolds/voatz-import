import com.mongodb.casbah.Imports._

import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.global._
import com.novus.salat.dao._

import scala.language.implicitConversions
import scala.language.postfixOps

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.async.Async.{async, await}
import scala.util.{Try, Success, Failure}

import mitek._

import akka.actor.ActorSystem
import akka.pattern.after

// Other interesting features: Ignore, Persist
case class MitekGetJobSettingsResult(@Key("_id") name: String, shortDescription: String,
  allowVideoFrames: Int, allowVideoFramesMode: Int,
  autoCaptureFailoverToStillCapture: Int, autoTorchAppearanceDelay: Int,
  autoTorchLowLightMinimum: Int, autoTorchSuspendProcessingTime: Int,
  cameraBrightness: Int, cameraDegreesThreshold: Int,
  cameraGuideImageAppearanceFillPercentage: Int, cameraGuideImageEnabled: Int,
  cameraGuideImageReappearanceDelay: Int, cameraGuideImageStillCameraAlpha: Int,
  cameraGuideImageStillCameraEnabled: Int, cameraInitialTimeoutInSeconds: Int,
  cameraMaxTimeouts: Int, cameraSharpness: Int, cameraTimeoutInSeconds: Int,
  cameraVideoAutoCaptureProcess: Int, cameraViewfinderMinFill: Int,
  cameraViewfinderMinHorizontalFill: Int, cameraViewfinderMinVerticalFill: Int,
  consecutiveVideoFrames: Int, lightingStillCamera: Int, lightingVideo: Int,
  requiredCompressionLevel: Int, requiredMaxImageHeightAndWidth: Int,
  requiredMaxImageSize: Int, screenRotationSuspendTime: Int, securityResult: Int,
  smartBubbleAppearanceDelay: Int, smartBubbleCumulativeErrorThreshold: Int,
  smartBubbleEnabled: Int, tutorialBrandNewUserDuration: Int, unnecessaryScreenTouchLimit: Int
)

case class MitekMpiVersion(@Key("_id") name: String, version: String)

object MitekGetJobSettingsResult {
  def fromScalaxbClass(o: GetJobSettingsResult): MitekGetJobSettingsResult = o match {
    case GetJobSettingsResult(
      securityResult, requiredMaxImageSize, name,
      shortDescription, allowVideoFrames, allowVideoFramesMode,
      autoCaptureFailoverToStillCapture, autoTorchAppearanceDelay,
      autoTorchLowLightMinimum, autoTorchSuspendProcessingTime,
      cameraBrightness, cameraDegreesThreshold,
      cameraGuideImageAppearanceFillPercentage, cameraGuideImageEnabled,
      cameraGuideImageReappearanceDelay, cameraGuideImageStillCameraAlpha,
      cameraGuideImageStillCameraEnabled, cameraInitialTimeoutInSeconds,
      cameraMaxTimeouts, cameraSharpness, cameraTimeoutInSeconds,
      cameraVideoAutoCaptureProcess, cameraViewfinderMinFill,
      cameraViewfinderMinHorizontalFill, cameraViewfinderMinVerticalFill,
      consecutiveVideoFrames, lightingStillCamera, lightingVideo,
      requiredCompressionLevel, requiredMaxImageHeightAndWidth,
      screenRotationSuspendTime, smartBubbleAppearanceDelay,
      smartBubbleCumulativeErrorThreshold, smartBubbleEnabled,
      tutorialBrandNewUserDuration, unnecessaryScreenTouchLimit) =>
    MitekGetJobSettingsResult(name.get, shortDescription.get, allowVideoFrames,
      allowVideoFramesMode, autoCaptureFailoverToStillCapture,
      autoTorchAppearanceDelay, autoTorchLowLightMinimum,
      autoTorchSuspendProcessingTime, cameraBrightness, cameraDegreesThreshold,
      cameraGuideImageAppearanceFillPercentage, cameraGuideImageEnabled,
      cameraGuideImageReappearanceDelay, cameraGuideImageStillCameraAlpha,
      cameraGuideImageStillCameraEnabled, cameraInitialTimeoutInSeconds,
      cameraMaxTimeouts, cameraSharpness, cameraTimeoutInSeconds,
      cameraVideoAutoCaptureProcess, cameraViewfinderMinFill,
      cameraViewfinderMinHorizontalFill, cameraViewfinderMinVerticalFill,
      consecutiveVideoFrames, lightingStillCamera, lightingVideo,
      requiredCompressionLevel, requiredMaxImageHeightAndWidth,
      requiredMaxImageSize, screenRotationSuspendTime,
      securityResult, smartBubbleAppearanceDelay,
      smartBubbleCumulativeErrorThreshold, smartBubbleEnabled,
      tutorialBrandNewUserDuration, unnecessaryScreenTouchLimit
    )
  }
}

case class MitekGetJobSettingsResultQuery(@Key("_id") name: Option[String] = None,
  shortDescription: Option[String] = None,
  allowVideoFrames: Option[Int] = None,
  allowVideoFramesMode: Option[Int] = None,
  autoCaptureFailoverToStillCapture: Option[Int] = None,
  autoTorchAppearanceDelay: Option[Int] = None,
  autoTorchLowLightMinimum: Option[Int] = None,
  autoTorchSuspendProcessingTime: Option[Int] = None,
  cameraBrightness: Option[Int] = None,
  cameraDegreesThreshold: Option[Int] = None,
  cameraGuideImageAppearanceFillPercentage: Option[Int] = None,
  cameraGuideImageEnabled: Option[Int] = None,
  cameraGuideImageReappearanceDelay: Option[Int] = None,
  cameraGuideImageStillCameraAlpha: Option[Int] = None,
  cameraGuideImageStillCameraEnabled: Option[Int] = None,
  cameraInitialTimeoutInSeconds: Option[Int] = None,
  cameraMaxTimeouts: Option[Int] = None,
  cameraSharpness: Option[Int] = None,
  cameraTimeoutInSeconds: Option[Int] = None,
  cameraVideoAutoCaptureProcess: Option[Int] = None,
  cameraViewfinderMinFill: Option[Int] = None,
  cameraViewfinderMinHorizontalFill: Option[Int] = None,
  cameraViewfinderMinVerticalFill: Option[Int] = None,
  consecutiveVideoFrames: Option[Int] = None,
  lightingStillCamera: Option[Int] = None,
  lightingVideo: Option[Int] = None,
  requiredCompressionLevel: Option[Int] = None,
  requiredMaxImageHeightAndWidth: Option[Int] = None,
  requiredMaxImageSize: Option[Int] = None,
  screenRotationSuspendTime: Option[Int] = None,
  securityResult: Option[Int] = None,
  smartBubbleAppearanceDelay: Option[Int] = None,
  smartBubbleCumulativeErrorThreshold: Option[Int] = None,
  smartBubbleEnabled: Option[Int] = None,
  tutorialBrandNewUserDuration: Option[Int] = None,
  unnecessaryScreenTouchLimit: Option[Int] = None
)

object MitekGetJobSettingsResultConversions {
  implicit def paramsToDBObject(params: MitekGetJobSettingsResultQuery): DBObject =
    grater[MitekGetJobSettingsResultQuery].asDBObject(params)

  implicit def jobSettingsResultToDBObject(r: MitekGetJobSettingsResult): DBObject =
    grater[MitekGetJobSettingsResult].asDBObject(r)
}

case class MitekGetJobSettingsResultDao(coll: MongoCollection) extends
  SalatDAO[MitekGetJobSettingsResult, String] (collection = coll)

case class MitekMpiVersionDao(coll: MongoCollection) extends
  SalatDAO[MitekMpiVersion, String] (collection = coll)

object JobSettingsImport extends App {
  implicit class Piper[A](val x: A) extends AnyVal { def |>[B](f: A => B) = f(x) }

  //TODO check mongoClient is opened
  implicit val client = MongoClient()
  val imp = SoapImport(client) //else XmlImport(MongoClient())
  imp.data |> imp.extract |> imp.persist
}

object SoapImport {
  implicit class Piper[A](val x: A) extends AnyVal { def |>[B](f: A => B) = f(x) }
  def apply(implicit client: MongoClient) = new SoapImport
}

class SoapImport(implicit mongoClient: MongoClient) {
  import SoapImport._
  val coll: MongoCollection = mongoClient("test")("test")
  implicit val system = ActorSystem("timeoutSystem")
  implicit val timeout = 3 seconds
  implicit class FutureExtensions[T](f: Future[T]) {
    def withTimeout(timeout: => Throwable)(implicit duration: FiniteDuration,
                                           system: ActorSystem): Future[T] = {
      Future firstCompletedOf Seq(f, after(duration, system.scheduler)(Future.failed(timeout)))
    }
  }

  def timeoutEx(msg: String) = new TimeoutException(msg)

  val service: ImagingPhoneServiceSoap =
    (new mitek.ImagingPhoneServiceSoap12Bindings
      with scalaxb.SoapClientsAsync
      with scalaxb.DispatchHttpClientsAsync {}).service

  val userName = Some("voatz@nimsim.com")
  val pass = Some("up2cFAVdQCzv")
  val phoneKey = Some("MitekTest")
  val orgName = Some("voatz")
  val collKey = "MPIVersion"

  val data: Future[AuthenticateUserResponse] = async {
    val checkConnection: Future[Boolean] =
      service.checkConnection() withTimeout timeoutEx("checkConneciton timeout")

    val authUser: Future[AuthenticateUserResponse] =
      service.authenticateUser(userName, pass, phoneKey, orgName) withTimeout
        timeoutEx("authenticate user timeout")

    await { checkConnection }
    await { authUser }
  }

  def extract(response: Future[AuthenticateUserResponse]): Future[AuthenticateEither] = {
    for {
      resp <- response
      option = for {
        result <- resp.AuthenticateUserResult
        mpiVersion <- result.MIPVersion
        securityCode = result.SecurityResult
        jobSettingsArray <- result.AvailableJobs
        res = if (securityCode == 0) Right(mpiVersion, jobSettingsArray) else Left(securityCode)
      } yield res
    } yield option
  }

  type AuthenticateEither = Option[Either[Int, (String, ArrayOfGetJobSettingsResult)]]
  def persist(data: Future[AuthenticateEither]): Unit = {
    data foreach { opt =>
      opt foreach { either =>
        val mpiVersionDao = MitekMpiVersionDao(coll)
        /* error handling for successfull future with an error security code */
        val tuple = either match {
          case Right(tuple) => Some(tuple)
          case Left(code) => println(s"error code: $code"); None
        }

        tuple foreach { case(version, availableJobs) =>
          val checked = mpiVersionDao.findOneById(collKey) map { _.version == version }
          checked match {
            case Some(true) => ;
            case Some(false) | None =>
              //coll.drop
              MitekMpiVersion(collKey, version) |> mpiVersionDao.insert
              availableJobs |> insert
          }
        }
      }
      shutdown()
    }

    /* caputes check conneciton and authenticate timeouts */
    for(exc <- data.failed) {
      println(exc.getMessage)
      shutdown()
    }
  }

  def insert(availableJobs: ArrayOfGetJobSettingsResult) = {
    val mitekJobSettingsResultDao = MitekGetJobSettingsResultDao(coll)
    for {
      seq <- availableJobs.GetJobSettingsResult
      el <- seq
    } yield {
      val obj = MitekGetJobSettingsResult.fromScalaxbClass(el)
      mitekJobSettingsResultDao.save(obj)
    }
    println("inserted new data")
  }

  def shutdown(): Unit = {
    //println(mitekJobSettingsResultDao.findOneById("DRIVER_LICENSE_UT"))
    //println(mpiVersionDao.findOneById("MPIVersion"))
    system.shutdown
    mongoClient.close
  }
}

object XmlImport {
  implicit class Piper[A](val x: A) extends AnyVal { def |>[B](f: A => B) = f(x) }
  def apply(implicit client: MongoClient) = new XmlImport
}

class XmlImport(implicit mongoClient: MongoClient) {
  import xml._
  //println(mitekJobSettingsResultDao.find(
    //MitekGetJobSettingsResultQuery(name = Some("DRIVER_LICENSE_UT"))).toIterable)

  val coll: MongoCollection = mongoClient("test")("test")

  // useful method just in case
  def clearScope(x: Node): Node = x match {
    case e:Elem => e.copy(scope=TopScope, child = e.child.map(clearScope))
    case o => o
  }

  val source = "./src/main/resources/AuthenticateUser.xml"
  val node = "GetJobSettingsResult"

  val data = loadXml(source)

  def loadXml(source: String): Node = {
    try {
      XML.loadFile(source)
    } catch {
      case e: Throwable => { println("* Error: File not found *"); sys.exit() }
    }
  }

  def getJobSettingsResults(xml: Node): Vector[MitekGetJobSettingsResult] = {
    (for(el <- (xml \\ node)) yield {
      val obj = scalaxb.fromXML[GetJobSettingsResult](el)
      MitekGetJobSettingsResult.fromScalaxbClass(obj)
    }).toVector
  }

  def extract(xml: Node) = {
    val jobSettings = getJobSettingsResults(xml)

    val securityResult = xml \\ "SecurityResult" // TODO: need to check 0 return code
    val version = xml \\ "MIPVersion"
    jobSettings
  }

  def persist(settings: Vector[MitekGetJobSettingsResult]) = {
    val mitekJobSettingsResultDao = MitekGetJobSettingsResultDao(coll)
    settings foreach { obj => mitekJobSettingsResultDao.insert(obj) }
  }
}
