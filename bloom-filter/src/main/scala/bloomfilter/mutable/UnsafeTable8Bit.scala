package bloomfilter.mutable

import scala.concurrent.util.Unsafe.{instance => unsafe}

class UnsafeTable8Bit(val numberOfBuckets: Long) {

  import UnsafeTable8Bit._

  private var random = 0
  private val tagsPerBucket = 4
  private val bytesPerBucket = (BitsPerItem * tagsPerBucket + 7) >> 3
  private val tagMask = (1L << BitsPerItem) - 1
  private val ptr = unsafe.allocateMemory(bytesPerBucket * numberOfBuckets)
  unsafe.setMemory(ptr, bytesPerBucket * numberOfBuckets, 0.toByte)

  def readTag(bucketIndex: Long, tagIndex: Int): Long = {
    val p = ptr + bucketIndex * bytesPerBucket + tagIndex
    val tag = unsafe.getByte(p)
    tag & tagMask
  }

  def writeTag(i: Long, j: Int, t: Long): Unit = {
    val p = ptr + i * bytesPerBucket
    val tag = t & tagMask
    unsafe.putByte(p + j, tag.toByte)
  }

  def insert(index: Long, tag: Long): Boolean = {
    var tagIndex = 0
    while (tagIndex < tagsPerBucket) {
      if (readTag(index, tagIndex) == EmptyTag) {
        writeTag(index, tagIndex, tag)
        return true
      }
      tagIndex += 1
    }

    false
  }

  def swapAny(index: Long, tag: Long): Long = {
    var tagIndex = 0
    while (tagIndex < tagsPerBucket) {
      if (readTag(index, tagIndex) == EmptyTag) {
        writeTag(index, tagIndex, tag)
        return EmptyTag
      }
      tagIndex += 1
    }

    random += 1
    val r =  random & (tagsPerBucket - 1)
    val tagToSwap = readTag(index, r)
    writeTag(index, r, tag)
    tagToSwap
  }

  def remove(index: Long, tag: Long): Boolean = {
    var tagIndex = 0
    while (tagIndex < tagsPerBucket) {
      if (readTag(index, tagIndex) == tag) {
        writeTag(index, tagIndex, EmptyTag)
        return true
      }
      tagIndex += 1
    }
    false
  }

  def find(index: Long, tag: Long): Boolean = {
    var i = 0
    while (i < tagsPerBucket) {
      val tag1 = readTag(index, i)
      if (tag1 == tag) {
        return true
      }
      i += 1
    }
    false
  }

  def dispose(): Unit = unsafe.freeMemory(ptr)
}

object UnsafeTable8Bit {
  val EmptyTag = 0
  val BitsPerItem = 8
}
