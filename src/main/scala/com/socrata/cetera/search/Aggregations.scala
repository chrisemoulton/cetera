package com.socrata.cetera.search

import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms

import com.socrata.cetera.types._

object Aggregations {

  val aggDomain =
    AggregationBuilders
      .terms("domains")
      .field(DomainFieldType.rawFieldName)
      .order(Terms.Order.count(false)) // count desc
      .size(0) // no docs, aggs only

  val aggCategories =
    AggregationBuilders
      .nested("annotations")
      .path(CategoriesFieldType.fieldName)
      .subAggregation(
        AggregationBuilders
          .terms("names")
          .field(CategoriesFieldType.Name.rawFieldName)
          .size(0)
      )

  val aggTags =
    AggregationBuilders
      .nested("annotations")
      .path(TagsFieldType.fieldName)
      .subAggregation(
        AggregationBuilders
          .terms("names")
          .field(TagsFieldType.Name.rawFieldName)
          .size(0)
      )

  val aggDomainCategory =
    AggregationBuilders
      .terms("categories")
      .field(DomainCategoryFieldType.rawFieldName)
      .order(Terms.Order.count(false)) // count desc
      .size(0) // no docs, aggs only

}
