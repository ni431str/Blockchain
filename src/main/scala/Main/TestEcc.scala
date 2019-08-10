package Main
import org.mongodb.scala.bson.ObjectId
import helper.Ecc.{CurvePoint, ECDSA, Key}

import scala.util.Random

/**
  * In this class the funtions of the Ecc algortihmen are tested to see how they are used
  */
object TestEcc {
  def main(args: Array[String]): Unit = {
    val text = "This text will be signed with secret, using UTF-8 encoding, SHA-256 hash and NIST p192 curve, and then verified"
    println("Private key (randomly generated):")

    // Just a random BigInt which is necessary to create a PrivateKey
    val secret = BigInt(212, new Random)
    // Create the Key with the before create random BigInt and a Curce of ur choosing, in the class ECDSA there are 4 different curves to choose from
    val key = Key.sec(secret, ECDSA.p192)
    println("Secret:" + secret)
    println(key)

    // Creation of PrivateKey with explicit values
    val numberX = BigInt.apply("99ba4022f12799c7132689d8eae039b1e9ca3046bbfefa2f".toUpperCase, 16)
    val numberY = BigInt.apply("e38e78d527657275f95c551f4ad2652b7772a70ab87e294".toUpperCase, 16)
    val numberSec = BigInt.apply("679721e8388d03173067e7cebf31e8e8651cbffda7c2a00cb1a6d".toUpperCase, 16)


    val testKey = new Key(new CurvePoint(ECDSA.p192, numberX, numberY), numberSec)
    println("TestKey \n" + testKey)

    println("\nsignature (is in BigInt form. Use Base64 or any other option to convert it to string, if you want):")

    // Creation of Signature of the given key, and text
    val sig = ECDSA.sign(key, text, "SHA-256")
    println(sig.toString(16))

    println("\npublic key in compresed form:")

    // Creation of an compressed PublicKey
    val pub = key.pub.compress
    println(pub.toString(16))

    println("\nuncompressed public key")
    // Creation of an uncompressed PublicKey, in form of x and y data
    val uncompressedKey = Key.pub(pub, ECDSA.p192)
    println(uncompressedKey)

    println("\nverification with uncompressed key (should be true):")

    // Verification is true if the signature was made with the same PrivateKey the uncompressedKey was made
    val verify = ECDSA.verify(sig, uncompressedKey, text)
    println(verify)
  }
}