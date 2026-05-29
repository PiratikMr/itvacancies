package org.example.core.util

import org.example.SparkEnv
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CacheTrackerTest extends AnyFlatSpec with Matchers with SparkEnv {

  import spark.implicits._

  private def makeTracker(): CacheTracker = new CacheTracker()

  "CacheTracker.register" should "return a cached DataFrame" in {
    val tracker = makeTracker()
    val df = Seq((1, "a")).toDF("id", "name")

    val cached = tracker.register(df)

    cached.storageLevel.useMemory shouldBe true
    cached.count() shouldBe 1
  }

  it should "allow registering multiple DataFrames" in {
    val tracker = makeTracker()
    val df1 = Seq((1, "a")).toDF("id", "name")
    val df2 = Seq((2, "b"), (3, "c")).toDF("id", "name")

    tracker.register(df1)
    tracker.register(df2)
  }

  "CacheTracker.clearAll" should "unpersist all registered DataFrames" in {
    val tracker = makeTracker()
    val df = Seq((1, "a")).toDF("id", "name")
    val cached = tracker.register(df)

    tracker.clearAll()

    cached.storageLevel.useMemory shouldBe false
  }

  it should "clear internal state so subsequent clearAll is safe" in {
    val tracker = makeTracker()
    tracker.register(Seq((1, "x")).toDF("id", "v"))
    tracker.clearAll()

    noException should be thrownBy tracker.clearAll()
  }
}
