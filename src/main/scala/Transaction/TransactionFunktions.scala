package Transaction

import java.security.MessageDigest
import org.mongodb.scala._
import helper.Ecc.{ECDSA, Key}
import helper.saveLoadKeyHelper.{getPrivateKeyFromLocation, getPublicKeyFromLocation}
import helper.ObservableHelpers._


object TransactionFunktions {

  /**
    * This function is used to create the connection to MongoDB
    *
    * @param userName the userName to log into MongoDB
    * @param database the database to log into MongoDB
    * @param password the password to log into MongoDB
    * @return MongoClient which is the connection to MongoDB
    */
  def createMongoClient(userName: String, database: String, password: String): MongoClient = {
    val connectionString = "mongodb://" + userName + ":" + password + "@localhost:27017/?authSource=" + database
    MongoClient(connectionString)
  }

  /**
    * This function is used to create the String for hashing a transaction
    *
    * @param sig the signuature of a transaction
    * @param value the value of a transaction
    * @param pub the PublicKey of the transaction
    * @param hash the hash of the transaction
    * @return the hashString
    */
  def createTransactionStringForHashing(sig: String, value: Int, pub: String, hash: String): String = {
    sig.concat(value.toString).concat(pub).concat(hash)
  }

  /**
    * This function hashes a transaction
    *
    * @param sig the signuature of a transaction
    * @param value the value of a transaction
    * @param pub the PublicKey of the transaction
    * @param hash the hash of the transaction
    * @return a hashed String of all values of the transaction
    */
  def hashTransaction(sig: BigInt, value: Int, pub: BigInt, hash: String): String = {
    MessageDigest.getInstance("SHA-256")
      .digest(createTransactionStringForHashing(sig.toString(16), value, pub.toString(16), hash).getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
  }

  /**
    * This funktion is used to get the hash of the last transaction to chain them together
    *
    * @param sig the signuature of a transaction
    * @param value the value of a transaction
    * @param pub the PublicKey of the transaction
    * @param collection the collection where the transaction should is safed
    * @return a hashed String of all values of the transaction with the hash of the last transaction of there is one
    */
  def getAndHashBefore(sig: BigInt, value: Int, pub: BigInt, collection: MongoCollection[Document]): String = {
    val document = collection.find().execute()
    val array = document.toArray
    if (!array.isEmpty) {
      val json = array(array.length - 1).toJson()
      val hash = json.slice(json.lastIndexOf("hash") + 8, json.lastIndexOf("value") - 2)
      hashTransaction(sig, value, pub, hash)
    } else {
      hashTransaction(sig, value, pub, "")
    }
  }

  /**
    * This funtction tests if there was a transaction before and if yes, verifies if the transaction can be inserted with giben PrivateKey
    *
    * @param key the PrivateKey of the transaction
    * @param collection the collection where the transaction should be inserted into
    * @return true if there are no transaction in the collection or if the verifycation is true
    */

  def checkForPubSigPair(key: Key, collection: MongoCollection[Document]): Boolean = {
    val document = collection.find().execute()
    val array = document.toArray
    if (!array.isEmpty) {
      val json = array(array.length - 1).toJson()
      val pubKeyTrans = json.slice(json.lastIndexOf("pub") + 7, json.length - 2)
      val valueTrans = json.slice(json.lastIndexOf("value") + 8, json.lastIndexOf("pub") - 3)
      val uncompressedKey = Key.pub(BigInt.apply(pubKeyTrans.toUpperCase, 16), ECDSA.p192)
      ECDSA.verify(ECDSA.sign(key, valueTrans, "SHA-256"), uncompressedKey, valueTrans)
    } else {
      true
    }
  }

  /**
    *
    * @param collection collection where to insert the transaction
    * @param locationPrivate the location of the PrivateKey (Transmitter)
    * @param locationPublic the location of the PublicKey (Receiver)
    * @param index the line in the PublicKey collection, to find the correct PublicKeys
    * @param value the value the transaction should be inserted with
    */
  def insertTransaction(collection: MongoCollection[Document], locationPrivate: String, locationPublic: String, index: Int, value: Int): Unit = {
    val docCount = collection.countDocuments().execute()
    val privateKey = getPrivateKeyFromLocation(locationPrivate)
    val pubKeyReceiver = getPublicKeyFromLocation(locationPublic, index)
    if (checkForPubSigPair(privateKey, collection)) {
      val sig = ECDSA.sign(privateKey, value.toString, "SHA-256")
      val pubKey = privateKey.pub.compress
      val hash = getAndHashBefore(sig, value, pubKey, collection: MongoCollection[Document])

      collection.insertOne(Document("_id" -> (docCount + 1).toInt, "sig" -> sig.toString(16), "hash" -> hash, "value" -> value, "pub" -> pubKeyReceiver.toString(16)))
        .subscribe((res: Completed) => println(res))
    } else {
      throw new Exception("This user is not allowed to insert a Transaction")
    }
  }
}

/*
object Transaction {
  def apply(_id: Int, sig: String, hash: String, value: Int, pub: String): Transaction =
    Transaction(new ObjectId(), sig, hash, value, pub)
}

case class Transaction(_id: ObjectId, sig: String, hash: String, value: Int, pub: String)

val codecRegistry = fromRegistries(fromProviders(classOf[Transaction]), DEFAULT_CODEC_REGISTRY )

val mongoClient: MongoClient = MongoClient()
val database: MongoDatabase = mongoClient.getDatabase("mydb").withCodecRegistry(codecRegistry)
val collection: MongoCollection[Person] = database.getCollection("test")

object Block {
  def apply (_id: Int, )
}*/
