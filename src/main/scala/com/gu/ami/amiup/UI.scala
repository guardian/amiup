package com.gu.ami.amiup

import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.services.autoscaling.model.InstanceRefreshStatus
import software.amazon.awssdk.services.cloudformation.model.Stack


object UI extends LazyLogging {
  def safeComplete(): Unit = {
    println("Update complete".colour(Console.GREEN))
    println("You should now deploy each stack to start machines with the new AMI")
  }

  def yoloComplete(): Unit = {
    println("Update complete".colour(Console.GREEN))
  }

  def error(message: String): Unit = {
    println("Aborting due to error".colour(Console.YELLOW))
    println(message.colour(Console.RED))
  }

  def confirmStacks(stacks: Seq[Stack]): Either[String, Seq[Stack]] = {
    println(s"Found ${stacks.size} stacks".colour(Console.GREEN))
    stacks.foreach { stack =>
      println(stack.stackName)
    }
    print("Update these stacks? [y/N]")
    val confirmation = scala.io.StdIn.readLine()
    if (confirmation.headOption.map(_.toLower).contains('y')) {
      println("Updating stacks...")
      Right(stacks)
    } else {
      Left("Cancelling at user request".colour(Console.YELLOW))
    }
  }

  def displayProgress(progress: Seq[StackProgress]): Unit = {
    progress.foreach {
      case StackProgress(stack, _, _, true) =>
        // stack update has failed
        println(s"${stack.stackName}: ${stack.stackStatus}".colour(Console.RED))
      case StackProgress(stack, true, true, _) =>
        println(s"${stack.stackName}: ${stack.stackStatus}".colour(Console.GREEN))
      case StackProgress(stack, true, false, _) =>
        println(s"${stack.stackName}: ${stack.stackStatus}".colour(Console.CYAN))
      case StackProgress(stack, false, false, _) =>
        println(s"${stack.stackName}: Starting...".colour(Console.BLUE))
      case StackProgress(stack, false, true, _) =>
        logger.error(s"stack ${stack.stackName} in invalid state, finished but not started")
        println(s"${stack.stackName}: ${stack.stackStatus}".colour(Console.CYAN))
    }
    println("-----------------------".colour(Console.RESET))
  }

  def displayRefreshProgress(refreshes: Seq[InstanceRefreshProgress]): Unit = {
    refreshes.foreach {
      case InstanceRefreshProgress(asgName, true, true, _, _, _) =>
        // Instance refresh has been cancelled or has failed
        println(s"$asgName: Instance refresh was not successful".colour(Console.RED))
      case InstanceRefreshProgress(asgName, false, _, _, _, _) =>
        println(s"$asgName: Starting instance refresh...".colour(Console.BLUE))
      case InstanceRefreshProgress(asgName, true, false, true, status, statusReason) =>
        println(s"$asgName: ${parseStatus(status, statusReason)}".colour(Console.GREEN))
      case InstanceRefreshProgress(asgName, true, false, _, status, statusReason) =>
        println(s"$asgName: ${parseStatus(status, statusReason)}".colour(Console.CYAN))
    }
    println("-----------------------".colour(Console.RESET))
  }

  private def parseStatus(status: Option[InstanceRefreshStatus], statusReason: Option[String]): String = {
    statusReason.fold(status.getOrElse("Unknown status").toString){ reason =>
      s"${status.getOrElse("Unknown status").toString} - $reason"
    }
  }

  implicit class RichString(val s: String) extends AnyVal {
    def colour(colour: String): String = {
      colour + s + Console.RESET
    }
  }
}
