package Main
import Transaction.TransactionFunktions._

object Test {
  def main(args: Array[String]): Unit = {
    //Testen abwechselnder User
    val client = createMongoClient("myUserAdmin", "admin", "abc123")
    val database = client.getDatabase("hr")
    val collection = database.getCollection("Main")
    /*insertTransaction(collection, "Privatekey_Savefile1", "PublicKey_Savefile2", 0, 2)
    insertTransaction(collection, "Privatekey_Savefile2", "PublicKey_Savefile1", 0, 5)*/

  }
}


