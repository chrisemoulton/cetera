package com.socrata.cetera.util

import com.socrata.cetera.types._
import org.scalatest.{FunSuiteLike, Matchers}

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

  test("'only' query parameter prohibits multiple selections") {
    qpp.restrictParamFilterType(Option("datasets,datalenses")) match {
      case Left(e: OnlyError) => ()
      case _ => fail("an OnlyError should be returned")
    }
  }
}
