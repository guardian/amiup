package com.gu.ami.amiup.util

import java.util.{Timer, TimerTask}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}


object RichFuture {
  def delay(delay: Duration): Future[Unit] = {
    val p = Promise[Unit]()
    val t = new Timer()
    t.schedule(new TimerTask {
      override def run(): Unit = p.complete(Success(()))
    }, delay.toMillis)
    p.future
  }

  implicit class RichFuture[A](fa: Future[A]) {
    def awaitAsEither[L](atMost: Duration)(f: Throwable => L): Either[L, A] = {
      Await.ready(fa, atMost).value.get match {
        case Success(t) => Right(t)
        case Failure(e) => Left(f(e))
      }
    }
  }
}
