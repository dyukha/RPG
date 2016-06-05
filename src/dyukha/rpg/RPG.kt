package dyukha.rpg;

import kotlin.system.exitProcess
import java.io.*;

fun main(args : Array<String>) {
  if (args.size != 2) {
    System.out.println("Usage: kotlin RPG <inputFile> <outputFile>");
    exitProcess(1);
  }
  val inputFile = args[0]
  val outputFile = args[1]

}
