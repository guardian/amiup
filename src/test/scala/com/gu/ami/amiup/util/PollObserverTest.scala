package com.gu.ami.amiup.util

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global


class PollObserverTest extends FreeSpec with Matchers {
  case class TestPollObserver() extends PollObserver[String, Int] {
    override val delay: Duration = 500.milliseconds
    override val initial: Int = 0
    override def poll()(implicit ec: ExecutionContext): Future[String] = {
      Future.successful("NowthisisthestoryallabouthowMylifegotflippedturnedupsidedownAndIdliketotakeaminutejustsitrightthereIlltellyouhowIbecametheprinceofatowncalledBelAir")
    }
    override def reduce(b: Int, a: String): Int = b + 1
    override def complete(b: Int)(implicit ec: ExecutionContext): Boolean = b == 5
  }

  "asFuture" - {
    "should take delay * 5" in {
      val startTime = DateTime.now().getMillis
      Await.result(TestPollObserver().toFuture(i => println(i)), 3.seconds)
      (DateTime.now().getMillis - startTime) should be >= 2500L
    }
  }
}
