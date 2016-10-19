package com.gu.ami.amiup.ui

import com.amazonaws.services.cloudformation.model.Stack
import com.gu.ami.amiup.StackProgress


object UI {
  def complete() = println("Update complete".colour(Console.GREEN))

  def confirmStacks(stacks: List[Stack]): Either[String, List[Stack]] = {
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
      Left("Aborting".colour(Console.YELLOW))
    }
  }

  def updateProgress(progress: List[StackProgress]): Unit = {
    progress.foreach { stackProgress =>
      val colour = stackProgress match {
        case StackProgress(_, true, true) =>
          Console.GREEN
        case StackProgress(_, true, false) =>
          Console.BLUE
        case _ =>
          Console.RESET
      }
      println(s"${stackProgress.stack.getStackName}: ${stackProgress.stack.getStackStatus}".colour(colour))
    }
    println("-----------------------")
  }

  implicit class RichString(val s: String) extends AnyVal {
    def colour(colour: String): String = {
      colour + s + Console.RESET
    }
  }
}
