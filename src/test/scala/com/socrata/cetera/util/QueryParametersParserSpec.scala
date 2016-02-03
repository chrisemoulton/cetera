package com.socrata.cetera.util

import org.scalatest.{FunSuiteLike, Matchers}

import com.socrata.cetera.types._

class QueryParametersParserSpec extends FunSuiteLike with Matchers {
  val qpp = QueryParametersParser

  test("validate a Right(Int)") {
    qpp.validated(Right(100)) should be (100)
  }

  test("'only' query parameter allows all datatypes") {
    Datatypes.all.foreach { datatype =>
      qpp.restrictParamFilterType(Some(datatype.plural)) should be(Right(Some(datatype.names)))
      qpp.restrictParamFilterType(Some(datatype.singular)) should be(Right(Some(datatype.names)))
    }
  }

  test("'only' query parameter specific instances") {
    qpp.restrictParamFilterType(Some("calendars")) should be(Right(Some(Seq("calendar"))))
    qpp.restrictParamFilterType(Some("datalenses")) should be(Right(Some(Seq("datalens"))))
    qpp.restrictParamFilterType(Some("datasets")) should be(Right(Some(Seq("dataset"))))
    qpp.restrictParamFilterType(Some("files")) should be(Right(Some(Seq("file"))))
    qpp.restrictParamFilterType(Some("filters")) should be(Right(Some(Seq("filter"))))
    qpp.restrictParamFilterType(Some("forms")) should be(Right(Some(Seq("form"))))
    qpp.restrictParamFilterType(Some("stories")) should be(Right(Some(Seq("story"))))

    val linksExpected = Seq("href")
    qpp.restrictParamFilterType(Some("links")) should be(Right(Some(linksExpected)))

    val chartsExpected = Seq("chart", "datalens_chart")
    qpp.restrictParamFilterType(Some("charts")) match {
      case Right(Some(ss)) => chartsExpected.foreach(t => ss should contain(t))
      case _ => fail()
    }

    val mapsExpected = Seq("datalens_map", "geo_map", "map", "tabular_map")
    qpp.restrictParamFilterType(Some("maps")) match {
      case Right(Some(ss)) => mapsExpected.foreach(t => ss should contain(t))
      case _ => fail()
    }
  }

  test("datatype boost parameter parses with valid datatype") {
    Params.datatypeBoostParam("boostDatasets") should contain(TypeDatasets)
  }

  test("datatype boost parameter ignore with invalid datatype") {
    Params.datatypeBoostParam("boostFoo") shouldBe empty
  }

  test("datatype boost parameter parsing is case-sensitive") {
    Params.datatypeBoostParam("BOOSTDATASETS") shouldBe empty
    Params.datatypeBoostParam("boostdatasets") shouldBe empty
    Params.datatypeBoostParam("boostDatasets") should contain(TypeDatasets)
  }

  test("field boosts are treated separately from datatype boosts") {
    val expectedFieldBoosts = Map(
        ColumnNameFieldType -> 10.0,
        ColumnDescriptionFieldType -> 10.0,
        ColumnFieldNameFieldType -> 10.0,
        TitleFieldType -> 9.0,
        DescriptionFieldType -> 8.0)

    val vqps = qpp(Map("boostColumns" -> "10.0", "boostTitle" -> "9.0", "boostDesc" -> "8.0").mapValues(Seq(_)))

    vqps match {
      case Left(_) => fail("a ValidatedQueryParameters should be returned")
      case Right(_) =>
    }

    vqps.right.get.fieldBoosts should be (expectedFieldBoosts)
    vqps.right.get.datatypeBoosts should be (Map())
  }

  test("datatype boosts are not treated as custom metadata key-value pairs") {
    val input = Map("boostCharts" -> "10.0", "foo" -> "bar").mapValues(Seq(_))
    val expected = Map("foo" -> "bar").mapValues(Seq(_))
    Params.remaining(input) should be(expected)
  }

  test("'only' query parameter prohibits multiple selections") {
    qpp.restrictParamFilterType(Option("datasets,datalenses")) match {
      case Left(e: OnlyError) => ()
      case _ => fail("an OnlyError should be returned")
    }
  }

  test("no datatype boost params results in empty datatype boosts map") {
    QueryParametersParser(Map("query" -> "crime").mapValues(Seq(_))) match {
      case Right(params) => params.datatypeBoosts should have size 0
      case _ => fail()
    }
  }

  test("malformed datatype boost params result in empty datatype boosts map") {
    QueryParametersParser(Map("query" -> "crime", "boostsDatasets" -> "5.0").mapValues(Seq(_))) match {
      case Right(params) => params.datatypeBoosts should have size 0
      case _ => fail()
    }
  }

  test("well-formed datatype boost params validate") {
    QueryParametersParser(Map("query" -> "crime", "boostDatasets" -> "5.0", "boostMaps" -> "2.0").mapValues(Seq(_))) match {
      case Right(params) => params.datatypeBoosts should have size 2
      case _ => fail()
    }
  }

  test("allow category with commas") {
    QueryParametersParser(Map("categories" -> "Traffic, Parking, and Transportation").mapValues(Seq(_))) match {
      case Right(params) =>
        params.categories should be('defined)
        params.categories.get should have size 1
        params.categories.get.head should be("Traffic, Parking, and Transportation")
      case _ => fail()
    }
  }

  test("allow tag with commas") {
    QueryParametersParser(Map("tags" -> "this, probably, doesn't, happen, on, any, customer, sites").mapValues(Seq(_))) match {
      case Right(params) =>
        params.tags should be('defined)
        params.tags.get should have size 1
        params.tags.get.head should be("this, probably, doesn't, happen, on, any, customer, sites")
      case _ => fail()
    }
  }

  test("allow multiple category parameters") {
    QueryParametersParser(Map("categories" -> Seq("Traffic", "Parking", "Transportation"))) match {
      case Right(params) =>
        params.categories should be('defined)
        params.categories.get should have size 3
        params.categories.get should contain theSameElementsAs Seq("Traffic", "Parking", "Transportation")
      case _ => fail()
    }
  }

  test("allow multiple tag parameters") {
    QueryParametersParser(Map("tags" -> Seq("Traffic", "Parking", "Transportation"))) match {
      case Right(params) =>
        params.tags should be('defined)
        params.tags.get should have size 3
        params.tags.get should contain theSameElementsAs Seq("traffic", "parking", "transportation")
      case _ => fail()
    }
  }

  test("also allow categories[] parameters") {
    QueryParametersParser(Map("categories" -> Seq("foo", "foos"), "categories[]" -> Seq("bar", "baz"))) match {
      case Right(params) =>
        params.categories should be('defined)
        params.categories.get should have size 4
        params.categories.get should contain theSameElementsAs Seq("foo", "foos", "bar", "baz")
      case _ => fail()
    }
  }

  test("also allow tags[] parameters") {
    QueryParametersParser(Map("tags" -> Seq("foo", "foos"), "tags[]" -> Seq("bar", "baz"))) match {
      case Right(params) =>
        params.tags should be('defined)
        params.tags.get should have size 4
        params.tags.get should contain theSameElementsAs Seq("foo", "foos", "bar", "baz")
      case _ => fail()
    }
  }

  test("domain metadata excludes known parameters") {
    val knownEnumParams = Map("only" -> Seq("calendar",
      "chart",
      "datalens",
      "dataset",
      "file",
      "filter",
      "form",
      "map",
      "href",
      "pulse",
      "story",
      "link",
      "datalens_chart",
      "datalens_map",
      "tabular_map"))

    val knownNumericParams = List(
      "boostColumns",
      "boostDesc",
      "boostTitle",
      "limit",
      "offset",
      "slop",
      "boostCalendars",
      "boostCharts",
      "boostDatalenses",
      "boostDatasets",
      "boostFiles",
      "boostFilters",
      "boostForms",
      "boostMaps",
      "boostHrefs",
      "boostPulses",
      "boostStories",
      "boostLinks",
      "boostDatalens_charts",
      "boostDatalens_maps",
      "boostTabular_maps"
    ).map(p => p -> Seq("42")).toMap

    val knownStringParams = List(
      "search_context",
      "domains",
      "categories",
      "categories[]",
      "tags",
      "tags[]",
      "q",
      "q_internal",
      "min_should_match",
      "show_feature_vals",
      "show_score",
      "function_score"
    ).map(p => p -> Seq(p)).toMap

    QueryParametersParser(knownEnumParams ++ knownNumericParams ++ knownStringParams) match {
      case Right(params) =>
        params.domainMetadata shouldNot be('defined)
      case _ => fail()
    }
  }

  // empty query string param is passed in from socrata-http multi params sometimes, e.g. catalog?q=bikes&
  test("handle empty query string param key") {
    QueryParametersParser(Map("" -> Seq())) match {
      case Right(params) => ()
      case _ => fail()
    }
  }

  // Q: what happens on &one+extra= ?
  // empty query string param is passed in from socrata-http multi params sometimes, e.g. catalog?q=bikes&one+extra
  test("handle empty query string param value") {
    QueryParametersParser(Map("one extra" -> Seq())) match {
      case Right(params) => ()
      case _ => fail()
    }
  }

  test("domain boosts can be parsed") {
    val domainBoosts = Map(
      "boostDomains[example.com]" -> Seq("1.23"),
      "boostDomains[data.seattle.gov]" -> Seq("4.56")
    )

    QueryParametersParser(domainBoosts) match {
      case Right(params) =>
        params.domainBoosts should be(Map("example.com" -> 1.23f, "data.seattle.gov" -> 4.56f))
      case _ => fail()
    }
  }

  test("domain boost defined twice will be completely ignored -- just documenting behavior") {
    val domainBoosts = Map(
      "boostDomains[example.com]" -> Seq("1.23", "2.34"),
      "boostDomains[data.seattle.gov]" -> Seq("4.56")
    )

    QueryParametersParser(domainBoosts) match {
      case Right(params) =>
        params.domainBoosts should be(Map("data.seattle.gov" -> 4.56f))
      case _ => fail()
    }
  }

  test("domain boosts missing fields do not explode the params parser") {
    val domainBoosts = Map(
      "boostDomains[data.seattle.gov]" -> Seq("4.56"),
      "boostDomains[example.com]" -> Seq(),
      "boostDomains[]" -> Seq("7.89"),
      "boostDomains[]" -> Seq()
    )

    QueryParametersParser(domainBoosts) match {
      case Right(params) =>
        params.domainBoosts should be(Map("data.seattle.gov" -> 4.56f))
      case _ => fail()
    }
  }

  test("domain boost degenerate cases do not explode the params parser") {
    val domainBoosts = Map(
      "boostDomains[boostDomains[example.com]]" -> Seq("1.23")
    )

    QueryParametersParser(domainBoosts) match {
      case Right(params) =>
        params.domainBoosts should be(Map("boostDomains[example.com]" -> 1.23f))
      case _ => fail()
    }
  }
}
