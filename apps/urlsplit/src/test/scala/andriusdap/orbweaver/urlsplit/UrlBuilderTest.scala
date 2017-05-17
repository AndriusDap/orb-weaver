package andriusdap.orbweaver.urlsplit

import andriusdap.orbweaver.executor.UrlBuilder
import org.scalatest.{FreeSpec, MustMatchers}

class UrlBuilderTest extends FreeSpec with MustMatchers {

  "app" - {
    "prefix dropper" - {

      "must not destroy links" in {
        UrlBuilder.dropPrefix("google.com") mustBe "google.com"
      }

      "must drop https://" in {
        UrlBuilder.dropPrefix("https://google.com") mustBe "google.com"
      }

      "must drop http://" in {
        UrlBuilder.dropPrefix("http://google.com") mustBe "google.com"
      }

      "must drop https://www." in {
        UrlBuilder.dropPrefix("https://www.google.com") mustBe "google.com"
      }

      "must drop http://www." in {
        UrlBuilder.dropPrefix("http://www.google.com") mustBe "google.com"
      }

      "must drop www." in {
        UrlBuilder.dropPrefix("www.google.com") mustBe "google.com"
      }
    }

    "split" - {
      "must not crash" in {
        val result =
          UrlBuilder.split("a3.twimg.com/profile_images/1129679871/twit_reasonably_small.jpg&description=After four seasons")

        result.host mustBe Vector("a3", "twimg", "com")
        result.query mustBe "profile images 1129679871 twit reasonably small jpg description after four seasons".split(" ").toVector
      }
    }
  }

}

