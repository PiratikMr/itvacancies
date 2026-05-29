package org.example.core.normalization.engine.model

import org.apache.spark.sql.Dataset

case class FuzzyMatcherResult(
                               matched: Dataset[FuzzyMatch],
                               toCreate: Dataset[FuzzyToCreate],
                               mappingData: Dataset[FuzzyMappingMeta],
                               clearCache: () => Unit
                             )
