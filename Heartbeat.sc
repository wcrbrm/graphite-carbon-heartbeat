import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import java.time.LocalTime
import java.util.{ Timer, TimerTask }

import java.io._
import scala.util.Properties

val graphiteHost = Properties.envOrElse("GRAPHITE_HOST", "127.0.0.1")
val graphitePort:Int = Integer.parseInt(Properties.envOrElse("GRAPHITE_PORT", "2003"))

case class DataChunk(metricPath: String, timestamp: Long, value: String) {
    def asString:String = s"${metricPath} ${value} ${timestamp}"
}
def sendData(chunks: List[DataChunk]) {
    try {
      val conn = new java.net.Socket(graphiteHost, graphitePort)
      conn.setSoTimeout(1000)
      val in = new BufferedReader(new InputStreamReader(conn.getInputStream))
      val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(conn.getOutputStream)))
      out.write(chunks.map(_.asString).mkString("\n") + "\n")
      out.flush
      conn.close
      println(" " + chunks.map(_.asString).mkString("\n ") + s"  (-> $graphiteHost:$graphitePort)")
    } catch {
      case e: java.net.ConnectException => println("ERROR:" + e.toString)
    }
}

object Heartbeat {
  def value(t: Int): Double = {
    val half:Double = t / 30.0
    Math.cos(half * 2.0 * Math.PI) * Math.cos((half + 0.5) * Math.PI)
  }

  def tick:Unit = { 
    val _now = LocalTime.now
    val min = _now.getMinute
    val second = _now.getSecond
    val timestamp: Long = new java.util.Date().getTime / 1000
    val funcValue = value(second)
    println(s"[$min:$second] $timestamp: " +  funcValue.toString)

    val chunk1 = DataChunk("test.heartbeat.thread1", timestamp, funcValue.toString)
    val chunk2 = DataChunk("test.heartbeat.thread2", timestamp, (0.25*funcValue).toString)
    sendData(List(chunk1, chunk2))
  }
}

do {
  Heartbeat.tick
  Thread.sleep(1000)
} while(true)