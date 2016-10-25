package com.gu.ami.amiup.util

import cats.data.EitherT
import rx.lang.scala.Observable

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.Duration
import scala.util.Success


/**
  * Structure to represent an operation that can be polled until it is completed.
  *
  * A - type returned when polling the source
  * B - type representing the current 'state'
  */
trait PollObserver[A, B] {
  val delay: Duration
  val initial: B

  def poll()(implicit ec: ExecutionContext): Future[A]

  /**
    * Convert each emitted value into some state,
    * based on the last state.
    */
  def reduce(b: B, a: A): B

  /**
    * Cease polling when `complete` evaluates to true based on the current state.
    */
  def complete(b: B)(implicit ec: ExecutionContext): Boolean

  def startPolling()(implicit ec: ExecutionContext): Observable[B] = {
    val delayedPoll = Observable.from {
      for {
        delay <- RichFuture.delay(delay)
        a <- poll()
      } yield a
    }
    delayedPoll
      .publish.refCount  // we only need one set of poll calls for the observable this function returns
      .repeat
      .scan(initial){ case (b, a) =>
        reduce(b, a)
      }
      .takeWhile(b => !complete(b))
  }

  def toFuture(onNext: B => Unit)(implicit ec: ExecutionContext): Future[Either[String, Unit]] = {
    val p = Promise[Either[String, Unit]]
    startPolling().subscribe(
      onNext,
      { err =>
        p.complete(Success(Left(err.getMessage)))
      },
      { () =>
        p.complete(Success(Right(())))
      }
    )
    p.future
  }
}
