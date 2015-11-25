package com.socrata.cetera.services

import com.rojoma.json.v3.ast.{JNumber, JString}
import com.rojoma.json.v3.codec.DecodeError
import com.rojoma.json.v3.interpolation._
import com.socrata.cetera.types.Count
import com.socrata.cetera.util.SearchResults
import org.scalatest.{FunSuiteLike, Matchers}
import com.socrata.cetera.search.{DomainSearchClient, TestESClient, ElasticSearchClient}

class CountServiceSpec extends FunSuiteLike with Matchers {
  val client: ElasticSearchClient = new TestESClient("CountService")
  val domainClient: DomainSearchClient = new DomainSearchClient(client.client)
  val service: CountService = new CountService(client, domainClient)

  val esResponse = j"""{
    "took" : 1,
    "timed_out" : false,
    "_shards" : {
      "total" : 10,
      "successful" : 10,
      "failed" : 0
    },
    "hits" : {
      "total" : 1862,
      "max_score" : 0.0,
      "hits" : [ ]
    },
    "aggregations" : {
      "domains" : {
        "doc_count_error_upper_bound" : 0,
        "sum_other_doc_count" : 0,
        "buckets" : [ {
          "key" : "onethousand.example.com",
          "doc_count" : 1000
        }, {
          "key" : "two-thirty-four.example.com",
          "doc_count" : 234
        }, {
          "key" : "seven-ate-nine.com",
          "doc_count" : 78
        }, {
          "key" : "poor-bono.example.com",
          "doc_count" : 1
        } ]
      }
    }
  }"""

  test("extract") {
    val expected = Stream(
      j"""{ "key" : "onethousand.example.com", "doc_count" : 1000 }""",
      j"""{ "key" : "two-thirty-four.example.com", "doc_count" : 234 }""",
      j"""{ "key" : "seven-ate-nine.com", "doc_count" : 78 }""",
      j"""{ "key" : "poor-bono.example.com", "doc_count" : 1 }"""
    )

    service.extract(esResponse) match {
      case Right(actual) =>
        (actual, expected).zipped.foreach{ (a, e) => a should be(e) }

      case Left(e) =>
        fail(e.toString)
    }
  }

  test("format") {
    val expected = SearchResults[Count](
      List(
        Count(JString("onethousand.example.com"), JNumber(1000)),
        Count(JString("two-thirty-four.example.com"),  JNumber(234)),
        Count(JString("seven-ate-nine.com"),  JNumber(78)),
        Count(JString("poor-bono.example.com"),  JNumber(1))
      )
    )

    service.extract(esResponse) match {
      case Right(extracted) =>
        val formatted = service.format(extracted)
        val results = formatted.results
        results.zip(expected.results).foreach{ case (a, e) => a should be (e) }

      case Left(e) =>
        fail(e.toString)
    }
  }

  test("return an error when the expected path to resources does not exist") {
    val body = j"""{}"""
    service.extract(body) match {
      case Right(aggregations) => fail("We should have returned a decode error!")
      case Left(error) => error match {
        case _: DecodeError =>
        case _ => fail("Expected a DecodeError")
      }
    }
  }

  ignore("timings") {}
  ignore("query parameters parser - errors") {}
  ignore("count request") {}

}