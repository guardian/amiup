package com.gu.ami.amiup.aws

import cats.data.EitherT
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient
import com.amazonaws.services.cloudformation.model.{Stack, StackStatus}
import com.gu.ami.amiup.StackProgress
import com.gu.ami.amiup.util.PollObserver
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success


case class PollDescribeStackStatus(stacks: Seq[Stack], delay: Duration, client: AmazonCloudFormationAsyncClient)
  extends PollObserver[Seq[Stack], Seq[StackProgress]] with LazyLogging {

  override val initial = stacks.map(StackProgress(_, started = false, finished = false))

  /**
    * Describes stacks to fetch the current stack update statuses.
    */
  override def poll()(implicit ec: ExecutionContext): Future[Seq[Stack]] = {
    AWS.describeStacks(client).map { dsr =>
      logger.debug("Fetched describeStacksResult")
      dsr.getStacks.asScala.toList.filter { stack =>
        stacks.exists(_.getStackId == stack.getStackId)
      }
    }
  }

  /**
    * Updates progress based on current status.
    */
  override def reduce(prevProgress: Seq[StackProgress], stackStatuses: Seq[Stack]): Seq[StackProgress] = {
    for {
      stackProgress <- prevProgress
      currentStatus <- stackStatuses.find(_.getStackId == stackProgress.stack.getStackId)
      // either we already saw it start or we can see it running
      started = stackProgress.started || !isFinished(currentStatus)
      // we've seen it start and now we see it has finished
      finished = stackProgress.started && isFinished(currentStatus)
    } yield StackProgress(currentStatus, started, finished)
  }

  /**
    * Stops polling when this is true.
    */
  override def complete(progress: Seq[StackProgress])(implicit ec: ExecutionContext): Boolean = {
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

  def asFuture(onNext: Seq[StackProgress] => Unit)(implicit ec: ExecutionContext): EitherT[Future, String, Unit] = {
    val p = Promise[Either[String, Unit]]
    startPolling().subscribe(
      onNext,
      { err =>
        logger.error("Error polling for updates", err)
        p.complete(Success(Left(err.getMessage)))
      },
      { () =>
        logger.debug("Polling completed")
        p.complete(Success(Right(())))
      }
    )
    EitherT(p.future)
  }
}
