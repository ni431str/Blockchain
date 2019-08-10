package helper

import java.io.{File, FileWriter, PrintWriter}

import helper.Ecc.{CurvePoint, ECDSA, Key}

import scala.reflect.io.Path
import scala.util.Random

object saveLoadKeyHelper {

  /**
    * A Test method to create 2 PrivateKeys and they're PublicKeys
    *
    * @param locationPrivate1 location to save the first PrivateKey
    * @param locationPrivate2 location to save the second PrivateKey
    * @param locationPublic location to save the PublicKeys of both PrivateKeys
    */
  def createTestPrivatePublicKeys(locationPrivate1: String, locationPrivate2: String, locationPublic: String): Unit = {
    saveNewPrivateKeyToLocation(locationPrivate1)
    saveNewPrivateKeyToLocation(locationPrivate2)
    val privateKey1 = getPrivateKeyFromLocation("Privatekey_Savefile1")
    val privateKey2 = getPrivateKeyFromLocation("Privatekey_Savefile2")
    savePublicKeyToLocation(locationPublic, privateKey1.pub.compress )
    savePublicKeyToLocation(locationPublic, privateKey2.pub.compress)

  }

  /**
    * Saving a PublicKey into a choosen location. More than 1 PublicKey in the same location, will be appended in a new line
    *
    * @param location The Location where the PublicKey should be safed
    * @param pubKey the Key which should be saved
    */
  def savePublicKeyToLocation(location: String, pubKey: BigInt):Unit = {
    val file = Path(location)
    if (file.exists) {
      val fw = new FileWriter(file.toString(), true)
      val index = io.Source.fromFile(location).getLines().size + 1
      fw.write(index.toString + ": " + pubKey.toString(16) + "\n")
      fw.close()
    } else {
      val fw = new FileWriter(new File(location))
      fw.write("1: " + pubKey.toString(16) + "\n")
      fw.close()
    }
  }

  /**
    * Get the PublicKey of choosen location
    *
    * @param location The Location where the publicKey is safed
    * @param index The Line in which the PublicKey is saved
    * @return The PublicKey in choosen location and line
    */
  def getPublicKeyFromLocation(location: String, index: Int) :BigInt = {
    val path = Path(location)
    if (path.exists) {
      val array = scala.io.Source.fromFile(location).getLines().toArray
      if(array.length - 1 >= index) BigInt.apply(array(index).substring(3).toUpperCase, 16) else throw new Exception("There is no Public Key with given index: " + index)
    } else {
      throw new Exception("File at Path " + location + "does not exists")
    }
  }

  /**
    * Creation of an new PrivateKey at given location
    *
    * @param location where to safe the PrivateKey
    */
  def saveNewPrivateKeyToLocation(location: String) : Unit = {
    val pw = new PrintWriter(new File(location))
    val secret = BigInt(212, new Random)
    val key = Key.sec(secret, ECDSA.p192)
    pw.write(key.toString)
    pw.close()
  }

  /**
    * Get the PrivateKey of choosen location
    *
    * @param location The location where the PrivateKey is safed
    * @return The PublicKey in choosen location
    */
  def getPrivateKeyFromLocation(location: String) :Key = {
    val path = Path(location)
    if (path.exists) {
      val array = scala.io.Source.fromFile(location).getLines().toArray
      val numberX = BigInt.apply(array(0).substring(3).toUpperCase, 16)
      val numberY = BigInt.apply(array(1).substring(3).toUpperCase, 16)
      val numberSec = BigInt.apply(array(2).substring(5).toUpperCase, 16)
      new Key(new CurvePoint(ECDSA.p192, numberX, numberY), numberSec)
    } else {
      throw new Exception("File at Path " + location + "does not exists")
    }
  }
}
