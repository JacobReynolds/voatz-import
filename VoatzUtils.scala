package org.nimsim.voatz

import scala.language.postfixOps
import akka.pattern.after
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import akka.actor.ActorSystem

object AsyncUtils {
  implicit class FutureExtensions[T](f: Future[T])(implicit system: ActorSystem) {
    def withTimeout(timeout: => Throwable)(implicit duration: FiniteDuration): Future[T] = {
      Future firstCompletedOf Seq(f, after(duration, system.scheduler)(Future.failed(timeout)))
    }
  }
  def timeoutEx(msg: String) = new TimeoutException(msg)
}
