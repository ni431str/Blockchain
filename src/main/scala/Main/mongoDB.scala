package Main
import java.util.concurrent.TimeUnit

import Transaction.TransactionFunktions._
import helper.ObservableHelpers._
import org.mongodb.scala.Observable
import org.mongodb.scala.bson.collection.immutable.Document

import scala.concurrent.Await
import scala.concurrent.duration.Duration
object Test {
  // Helper classes from MongoDB for correctly using transaktions
  implicit class DocumentObservable[C](val observable: Observable[Document]) extends ImplicitObservable[Document] {
    override val converter: (Document) => String = (doc) => doc.toJson
  }

  implicit class GenericObservable[C](val observable: Observable[C]) extends ImplicitObservable[C] {
    override val converter: (C) => String = (doc) => doc.toString
  }

  trait ImplicitObservable[C] {
    val observable: Observable[C]
    val converter: (C) => String

    def results(): Seq[C] = Await.result(observable.toFuture(), Duration(10, TimeUnit.SECONDS))
    def headResult() = Await.result(observable.head(), Duration(10, TimeUnit.SECONDS))
    def printResults(initial: String = ""): Unit = {
      if (initial.length > 0) print(initial)
      results().foreach(res => println(converter(res)))
    }
    def printHeadResult(initial: String = ""): Unit = println(s"${initial}${converter(headResult())}")
  }


  def main(args: Array[String]): Unit = {
    //Testen abwechselnder User
    val client = createMongoClient("myUserAdmin", "admin", "abc123")
    val database = client.getDatabase("hr")
    val collection = database.getCollection("Transaktion")
    //insertTransaction(collection, "Privatekey_Savefile1", "PublicKey_Savefile2", 0, 5)
    //insertTransaction(collection, "Privatekey_Savefile2", "PublicKey_Savefile1", 0, 5)
  }
}