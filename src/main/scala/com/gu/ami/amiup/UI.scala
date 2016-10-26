package com.gu.ami.amiup

import com.amazonaws.services.cloudformation.model.Stack
import com.typesafe.scalalogging.LazyLogging


object UI extends LazyLogging {
  def complete() = {
    println("Update complete".colour(Console.GREEN))
    println("You should now deploy each stack to start machines with the new AMI")
  }

  def error(message: String) = {
    println("Aborting due to error".colour(Console.YELLOW))
    println(message.colour(Console.RED))
  }

  def confirmStacks(stacks: Seq[Stack]): Either[String, Seq[Stack]] = {
    println(s"Found ${stacks.size} stacks".colour(Console.GREEN))
    stacks.foreach { stack =>
      println(stack.getStackName)
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
      case StackProgress(stack, true, true) =>
        println(s"${stack.getStackName}: ${stack.getStackStatus}".colour(Console.GREEN))
      case StackProgress(stack, true, false) =>
        println(s"${stack.getStackName}: ${stack.getStackStatus}".colour(Console.CYAN))
      case StackProgress(stack, false, false) =>
        println(s"${stack.getStackName}: Starting...".colour(Console.BLUE))
      case StackProgress(stack, false, true) =>
        logger.error(s"stack ${stack.getStackName} in invalid state, finished but not started")
        println(s"${stack.getStackName}: ${stack.getStackStatus}".colour(Console.CYAN))
    }
    println("-----------------------".colour(Console.RESET))
  }

  implicit class RichString(val s: String) extends AnyVal {
    def colour(colour: String): String = {
      colour + s + Console.RESET
    }
  }
}
