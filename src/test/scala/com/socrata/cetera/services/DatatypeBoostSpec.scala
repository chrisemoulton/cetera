package com.socrata.cetera.services

import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import com.rojoma.json.v3.ast.{JNumber, JString}

import com.socrata.cetera.search.{DomainSearchClient, ElasticSearchClient, TestESClient, TestESData}
import com.socrata.cetera.types._
import com.socrata.cetera.util.Params

class DatatypeBoostSpec extends FunSuiteLike with Matchers with TestESData with BeforeAndAfterAll {
  val boostedDatatype = TypeCharts
  val client: ElasticSearchClient = new TestESClient(testSuiteName, Map(boostedDatatype -> 10F))
  val domainClient: DomainSearchClient = new DomainSearchClient(client.client)
  val service: SearchService = new SearchService(client, domainClient)

  override protected def beforeAll(): Unit = {
    bootstrapData()
  }

  override protected def afterAll(): Unit = {
    removeBootstrapData()
    client.close()
  }

  test("datatype boost - increases score when datatype matches") {
    val (results, _) = service.doSearch(Map(
      Params.querySimple -> "best",
      Params.showScore -> "true"
    ))
    val oneBoosted = results.results.find(_.resource.dyn.`type`.! == JString(boostedDatatype.singular)).head
    val oneOtherThing = results.results.find(_.resource.dyn.`type`.! != JString(boostedDatatype.singular)).head

    val datalensScore: Float = oneBoosted.metadata("score") match {
      case n: JNumber => n.toFloat
      case _ => fail()
    }
    val otherScore: Float = oneOtherThing.metadata("score") match {
      case n: JNumber => n.toFloat
      case _ => fail()
    }

    datalensScore should be > otherScore
  }
}