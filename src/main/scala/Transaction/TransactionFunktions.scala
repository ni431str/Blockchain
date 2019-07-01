package Transaction

import java.security.MessageDigest
import org.mongodb.scala._
import helper.Ecc.{ECDSA, Key}
import helper.saveLoadKeyHelper.{getPrivateKeyFromLocation, getPublicKeyFromLocation}
import helper.ObservableHelpers._


object TransactionFunktions {

  def createMongoClient(userName: String, database: String, password: String): MongoClient = {
    val connectionString = "mongodb://" + userName + ":" + password + "@localhost:27017/?authSource=" + database
    MongoClient(connectionString)
  }

  def createTransactionStringForHashing(sig: String, value: Int, pub: String, hash: String): String = {
    sig.concat(value.toString).concat(pub).concat(hash)
  }

  def hashTransaction(sig: BigInt, value: Int, pub: BigInt, hash: String): String = {
    MessageDigest.getInstance("SHA-256")
      .digest(createTransactionStringForHashing(sig.toString(16), value, pub.toString(16), hash).getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
  }

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

