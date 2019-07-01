package helper

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

import org.mongodb.scala.{MongoTimeoutException, Observable, Observer, SingleObservable, Subscription}
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration


object ObservableHelpers {
  val waitDuration = Duration(5, "seconds")

  implicit class ObservableExecutor[T](observable: Observable[T]) {
    def execute(): Seq[T] = Await.result(observable.toFuture(), waitDuration)

    def subscribeAndAwait(): Unit = {
      val observer: LatchedObserver[T] = new LatchedObserver[T](false)
      observable.subscribe(observer)
      observer.await()
    }
  }

  implicit class SingleObservableExecutor[T](observable: SingleObservable[T]) {
    def execute(): T = Await.result(observable.toFuture(), waitDuration)
  }

  class LatchedObserver[T](val printResults: Boolean = true, val minimumNumberOfResults: Int = 1) extends Observer[T] {
    private val latch: CountDownLatch = new CountDownLatch(1)
    private val resultsBuffer: mutable.ListBuffer[T] = new mutable.ListBuffer[T]
    private var subscription: Option[Subscription] = None
    private var error: Option[Throwable] = None

    override def onSubscribe(s: Subscription): Unit = {
      subscription = Some(s)
      s.request(Integer.MAX_VALUE)
    }

    override def onNext(t: T): Unit = {
      resultsBuffer.append(t)
      if (printResults) println(t)
      if (resultsBuffer.size >= minimumNumberOfResults) latch.countDown()
    }

    override def onError(t: Throwable): Unit = {
      error = Some(t)
      println(t.getMessage)
      onComplete()
    }

    override def onComplete(): Unit = {
      latch.countDown()
    }

    def results(): List[T] = resultsBuffer.toList

    def await(): Unit = {
      if (!latch.await(60, SECONDS)) throw new MongoTimeoutException("observable timed out")
      if (error.isDefined) throw error.get
    }

    def waitForThenCancel(): Unit = {
      if (minimumNumberOfResults > resultsBuffer.size) await()
      subscription.foreach(_.unsubscribe())
    }
  }
}
