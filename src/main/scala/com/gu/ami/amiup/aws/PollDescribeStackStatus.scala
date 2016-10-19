package com.gu.ami.amiup.aws

import cats.data.EitherT
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient
import com.amazonaws.services.cloudformation.model.{Stack, StackStatus}
import com.gu.ami.amiup.StackProgress
import com.gu.ami.amiup.util.PollObserver

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.util.Success


case class PollDescribeStackStatus(stacks: List[Stack], delay: Duration, client: AmazonCloudFormationAsyncClient)
  extends PollObserver[List[Stack], List[StackProgress]] {

  override val initial = stacks.map(StackProgress(_, started = false, finished = false))

  override def poll()(implicit ec: ExecutionContext): Future[List[Stack]] = {
    AWS.describeStacks(client).map { dsr =>
      dsr.getStacks.asScala.toList.filter(stack => stacks.exists(_.getStackId == stack.getStackId))
    }
  }

  override def reduce(prevProgress: List[StackProgress], stackStatuses: List[Stack]): List[StackProgress] = {
    for {
      stackProgress <- prevProgress
      currentStatus <- stackStatuses.find(_.getStackId == stackProgress.stack.getStackId)
      // either we already saw it start or we can see it running
      started = stackProgress.started || !isFinished(currentStatus)
      // we've seen it start and now we see it has finished
      finished = stackProgress.started && isFinished(currentStatus)
    } yield StackProgress(currentStatus, started, finished)
  }

  override def complete(progress: List[StackProgress])(implicit ec: ExecutionContext): Boolean = {
    progress.forall { case StackProgress(_, started, finished) =>
      started && finished
    }
  }

  def isFinished(stack: Stack): Boolean = {
    StackStatus.fromValue(stack.getStackStatus) match {
      case StackStatus.UPDATE_COMPLETE => true
      case StackStatus.ROLLBACK_COMPLETE => true
      case StackStatus.UPDATE_ROLLBACK_FAILED => true
      case _ => false
    }
  }

  def asFuture(onNext: List[StackProgress] => Unit)(implicit ec: ExecutionContext): EitherT[Future, String, Unit] = {
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
    EitherT(p.future)
  }
}
