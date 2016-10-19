package com.gu.ami.amiup.util

import rx.lang.scala.Observable

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration


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
    * Cease polling when the state reaches
    */
  def complete(b: B)(implicit ec: ExecutionContext): Boolean

  def startPolling()(implicit ec: ExecutionContext) = {
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
}
