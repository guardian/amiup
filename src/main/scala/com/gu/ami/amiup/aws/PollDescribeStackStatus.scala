package com.gu.ami.amiup.aws

import cats.data.EitherT
import cats.instances.future._
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient
import com.amazonaws.services.cloudformation.model.{Stack, StackStatus}
import com.gu.ami.amiup.StackProgress
import com.gu.ami.amiup.util.RichFuture
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


object PollDescribeStackStatus extends LazyLogging {
  val interval = 2.seconds

  def pollUntilComplete(stacks: Seq[Stack], client: AmazonCloudFormationAsyncClient)(onNext: Seq[StackProgress] => Unit)(implicit ec: ExecutionContext): EitherT[Future, String, Unit] = {
    def loop(progress: Seq[StackProgress]): EitherT[Future, String, Unit] = {
      onNext(progress)

      if (complete(progress)) {
        // stop looping when we're finished
        logger.info("Polling complete, stacks are updated")
        EitherT.pure[Future, String, Unit](())
      } else {
        // poll again, after a delay
        logger.debug("Polling not yet complete, will repeat lookup after delay")
        for {
          // delay next execution
          _ <- EitherT.right(RichFuture.delay(interval))
          // call describe stacks to get current status
          stackStatuses <- getStackStatuses(stacks, client)
          // update progress
          nextProgress = updateProgress(progress, stackStatuses)
          // start again
          next <- loop(nextProgress)
        } yield next
      }
    }
    val initialProgress = stacks.map(StackProgress(_, started = false, finished = false, failed = false))
    loop(initialProgress)
  }

  private def getStackStatuses(stacks: Seq[Stack], client: AmazonCloudFormationAsyncClient)(implicit ec: ExecutionContext): EitherT[Future, String, Seq[Stack]] = {
    EitherT {
      AWS.describeStacks(client).map { dsr =>
        logger.debug("Fetched describeStacksResult")
        Right(dsr.getStacks.asScala.toList.filter { stack =>
          stacks.exists(_.getStackId == stack.getStackId)
        })
      }.recover { case err =>
        logger.error("Error describing stacks", err)
        Left(err.getMessage)
      }
    }
  }

  private[aws] def updateProgress(prevProgress: Seq[StackProgress], stackStatuses: Seq[Stack]): Seq[StackProgress] = {
    for {
      stackProgress <- prevProgress
      currentStatus <- stackStatuses.find(_.getStackId == stackProgress.stack.getStackId)
      // either we already saw it start or we can see it running
      started = stackProgress.started || !isFinished(currentStatus)
      // we've seen it start and now we see it has finished
      finished = stackProgress.started && isFinished(currentStatus)
      // if we've seen it start a failure status means we've broken it
      failed = stackProgress.started && isFailed(currentStatus)
    } yield StackProgress(currentStatus, started, finished, failed)
  }

  private[aws] def complete(progress: Seq[StackProgress]): Boolean = {
    progress.forall { case StackProgress(_, started, finished, _) =>
      started && finished
    }
  }

  private[aws] def isFinished(stack: Stack): Boolean = {
    StackStatus.fromValue(stack.getStackStatus) match {
      case StackStatus.ROLLBACK_COMPLETE => true
      case StackStatus.ROLLBACK_FAILED => true
      case StackStatus.CREATE_COMPLETE => true
      case StackStatus.DELETE_FAILED => true
      case StackStatus.UPDATE_COMPLETE => true
      case StackStatus.UPDATE_ROLLBACK_FAILED => true
      case StackStatus.UPDATE_ROLLBACK_COMPLETE => true
      case _ => false
    }
  }

  private[aws] def isFailed(stack: Stack): Boolean = {
    StackStatus.fromValue(stack.getStackStatus) match {
      case StackStatus.UPDATE_ROLLBACK_IN_PROGRESS => true
      case StackStatus.UPDATE_ROLLBACK_FAILED => true
      case StackStatus.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS => true
      case StackStatus.UPDATE_ROLLBACK_COMPLETE => true
      case _ => false
    }
  }
}
