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
import scala.util.{Try, Success, Failure}
import scala.async.Async.{async, await}

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

case class MitekMpiVersion(@Key("_id") name: String, version: String)

case class MitekMpiVersionDao(coll: MongoCollection) extends
  SalatDAO[MitekMpiVersion, String] (collection = coll)

object XmlImport extends App {
 import MitekGetJobSettingsResultConversions._

 // Config: userName, pass, phoneKey, orgName, timeout, documentKey, db, collection
  implicit val system = ActorSystem("timeoutSystem")
  implicit val timeout = 3 seconds
  implicit class FutureExtensions[T](f: Future[T]) {
    def withTimeout(timeout: => Throwable)(implicit duration: FiniteDuration,
                                           system: ActorSystem): Future[T] = {
      Future firstCompletedOf Seq(f, after(duration, system.scheduler)(Future.failed(timeout)))
    }
  }

  def timeoutEx(msg: String) = new TimeoutException(msg)

  val mongoClient: MongoClient = MongoClient()
  val coll: MongoCollection = mongoClient("test")("test")

  val mitekJobSettingsResultDao = MitekGetJobSettingsResultDao(coll)
  val mpiVersionDao = MitekMpiVersionDao(coll)

  val service: ImagingPhoneServiceSoap =
    (new mitek.ImagingPhoneServiceSoap12Bindings
      with scalaxb.SoapClientsAsync
      with scalaxb.DispatchHttpClientsAsync {}).service

  val userName = Some("voatz@nimsim.com")
  val pass = Some("up2cFAVdQCzv")
  val phoneKey = Some("MitekTest")
  val orgName = Some("voatz")


  val response: Future[AuthenticateUserResponse] = async {
    /*
    val checkConnection: Future[Boolean] =
      service.checkConnection() withTimeout timeoutEx("checkConneciton timeout")
    */
    val fetchSettings: Future[AuthenticateUserResponse] =
      service.authenticateUser(userName, pass, phoneKey, orgName) withTimeout
        timeoutEx("authenticate user timeout")

    await { fetchSettings }
    /*
    val check: Boolean = await { checkConnection }
    if (check == true) await { fetchSettings }
    else throw timeoutEx("checkConection timeout")
    */
  }

  def extract(response: Future[AuthenticateUserResponse]) =
    response map { resp =>
      for {
        result <- resp.AuthenticateUserResult
        mpiVersion <- result.MIPVersion
        securityResult = result.SecurityResult
        jobSettingsArray <- result.AvailableJobs
      } yield (mpiVersion, jobSettingsArray)
    }

  def persist(data: Future[Option[(String, ArrayOfGetJobSettingsResult)]]): Unit = {
    def insert(availableJobs: ArrayOfGetJobSettingsResult) = {
      for {
        seq <- availableJobs.GetJobSettingsResult
        el <- seq
      } yield {
        val obj = MitekGetJobSettingsResult.fromScalaxbClass(el)
        mitekJobSettingsResultDao.insert(obj)
      }
      println("inserted something new")
    }

    data foreach { opt =>
      val (version, availableJobs) = opt match {
        case Some(tuple) => tuple
        case None => ("", ArrayOfGetJobSettingsResult())
      }

      mpiVersionDao.findOneById("MPIVersion") match {
        case None =>
          mpiVersionDao.insert(MitekMpiVersion("MPIVersion", version))
          insert(availableJobs)
        case Some(v) =>
          if (v.version != version) {
            coll.drop
            insert(availableJobs)
          }
      }
      shutdown()
    }

    for(exc <- data.failed) {
      println(exc.getMessage)
      shutdown()
    }
  }

  def shutdown(): Unit = {
    println(mitekJobSettingsResultDao.findOneById("DRIVER_LICENSE_UT"))
    println(mpiVersionDao.findOneById("MPIVersion"))
    system.shutdown
    mongoClient close
  }

  persist(extract(response))

/*
  //println(mitekJobSettingsResultDao.find(
    //MitekGetJobSettingsResultQuery(name = Some("DRIVER_LICENSE_UT"))).toIterable)

  importXML("./src/main/resources/AuthenticateUser.xml", "GetJobSettingsResult")

  def getJobSettingsResults(xml: Node, node: String): Vector[MitekGetJobSettingsResult] = {
    (for(el <- (xml \\ node)) yield {
      val obj = scalaxb.fromXML[GetJobSettingsResult](el)
      MitekGetJobSettingsResult.fromScalaxbClass(obj)
    }).toVector
  }

  def importXML(source: String, node: String) = {
    val xml = loadXml(source)
    val jobSettings = getJobSettingsResults(xml, node)
    val securityResult = xml \\ "SecurityResult" // TODO: need to check 0 return code
    val version = xml \\ "MIPVersion"
    //println(jobSettingsResultToDBObject(jobSettings.head))
    val save = persist(jobSettings)
  }

  def loadXml(source: String): Node = {
    try {
      XML.loadFile(source)
    } catch {
      case e: Throwable => { println("* Error: File not found *"); sys.exit() }
    }
  }

  // useful method just in case
  def clearScope(x: Node): Node = x match {
    case e:Elem => e.copy(scope=TopScope, child = e.child.map(clearScope))
    case o => o
  }

*/

}
