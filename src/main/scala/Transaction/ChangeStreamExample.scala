package Transaction

import com.mongodb.client.model.changestream.FullDocument
import helper.ObservableHelpers.LatchedObserver
import org.mongodb.scala.Document
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.changestream.ChangeStreamDocument
import TransactionFunktions._
import helper.ObservableHelpers._

object ChangeStreamExample {

  def testUpdateAndDeleteFunction() ={
    val mongoClient = createMongoClient("myUserAdmin", "admin", "abc123")
    val database = mongoClient.getDatabase("hr")
    val collection = database.getCollection("lol")
    val observable = collection.watch.fullDocument(FullDocument.UPDATE_LOOKUP)
    val observer = new LatchedObserver[ChangeStreamDocument[Document]]()
    observable.subscribe(observer)
    val docOld = collection.find(Filters.eq("username", "alice123")).first().execute()
    collection.updateOne(Document("{username: 'alice123'}"), Document("{$set : { email: 'NickTest2@example.com'}}")).subscribeAndAwait()
    observer.waitForThenCancel()

    val results = observer.results()
    if(results.head.getOperationType.getValue.equals("update") || results.head.getOperationType.getValue.equals("delete") || results.head.getOperationType.getValue.equals("replace")) {
      collection.replaceOne(Document("{username: 'alice123'}"), docOld).subscribeAndAwait()
    }
  }
}
