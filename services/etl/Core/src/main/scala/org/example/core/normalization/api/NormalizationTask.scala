package org.example.core.normalization.api

import org.example.core.normalization.model.NormalizersEnum.{GroupNonHierarchical, NormalizerType}

import scala.language.implicitConversions

sealed trait NormalizationTask {
  def nType: NormalizerType
}

object NormalizationTask {
  case class Standard(nType: NormalizerType) extends NormalizationTask
  case class Exact(nType: NormalizerType) extends NormalizationTask

  case class ExtractTags(nType: GroupNonHierarchical, sourceCol: String) extends NormalizationTask

  implicit def typeToStandard(nType: NormalizerType): NormalizationTask = Standard(nType)
}