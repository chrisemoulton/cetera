package com.socrata.cetera.search

import scala.collection.JavaConverters._

import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import com.socrata.cetera._
import com.socrata.cetera.types._

class TestESDataSpec extends FunSuiteLike with Matchers with TestESData with BeforeAndAfterAll {
  val client: ElasticSearchClient = new TestESClient(testSuiteName)

  override protected def beforeAll(): Unit = {
    bootstrapData()
  }

  override protected def afterAll(): Unit = {
    removeBootstrapData()
    client.close()
  }

  test("aliases are bootstrapped") {
    val res = client.client.admin().indices().prepareGetAliases()
      .execute.actionGet
    val indexAliases = res.getAliases.asScala.map(ooc => ooc.key -> ooc.value.asScala.map(_.alias))
    indexAliases.size should be(1) // number of indices
    indexAliases.flatMap(as => as._2).size should be(1) // number of aliases
    Indices.foreach { index =>
      indexAliases.find { case (_,as) => as.contains(index) } should be('defined)
    }
  }

  test("settings are bootstrapped") {
    val res = client.client.admin().indices().prepareGetSettings()
      .execute.actionGet
    val settings = res.getIndexToSettings.asScala.map(ooc => ooc.key -> ooc.value)
    settings.size should be(2)
    settings.find { case (i,_) => i == testSuiteName } should be('defined)
    settings.find { case (i,_) => i == ".scripts" } should be('defined)
  }

  test("mappings are bootstrapped") {
    val res = client.client.admin().indices().prepareGetMappings()
      .execute.actionGet
    val mappings = res.getMappings.asScala.flatMap { ooc =>
      ooc.value.asScala.map { iom =>
        iom.key -> iom.value.sourceAsMap().asScala
      }
    }
    mappings.size should be(4)
    mappings.find(_._1 == "document") should be('defined)
    mappings.find(_._1 == "domain") should be('defined)
    mappings.find(_._1 == "expression") should be('defined)
    mappings.find(_._1 == "groovy") should be('defined)
  }

  test("test docs are bootstrapped") {
    val res = client.client.prepareSearch(Indices :_*).execute.actionGet
    val numDocs = Datatypes.materialized.length
    val numDomains = domainCnames.length
    res.getHits.getTotalHits should be(numDocs + numDomains)
  }
}
