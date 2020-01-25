package ml.combust.mleap.xgboost.runtime

import biz.k11i.xgboost.Predictor
import biz.k11i.xgboost.util.FVec
import ml.combust.mleap.core.classification.ProbabilisticClassificationModel
import org.apache.spark.ml.linalg.{Vector, Vectors}
import XgbConverters._
import ml.combust.mleap.core.types.{ListType, ScalarType, StructType, TensorType}


trait XGBoostPerformantClassificationModelBase extends ProbabilisticClassificationModel {
  def predictor: Predictor
  def treeLimit: Int

  override def predict(features: Vector): Double = predict(features.asXGBPredictor)
  def predict(data: FVec): Double

  override def predictRaw(features: Vector): Vector = predictRaw(features.asXGBPredictor)
  def predictRaw(data: FVec): Vector

  override def predictProbabilities(features: Vector): Vector = predictProbabilities(features.asXGBPredictor)
  def predictProbabilities(data: FVec): Vector

  def predictLeaf(features: Vector): Seq[Double] = predictLeaf(features.asXGBPredictor)
  def predictLeaf(data: FVec): Seq[Double] = predictor.predictLeaf(data, treeLimit).map(_.toDouble)

}

case class XGBoostPerformantBinaryClassificationModel(
      override val predictor: Predictor,
      override val numFeatures: Int,
      override val treeLimit: Int) extends XGBoostPerformantClassificationModelBase {

  override val numClasses: Int = 2

  def predict(data: FVec): Double =
    Math.round(predictor.predict(data, false, treeLimit).head)

  def predictProbabilities(data: FVec): Vector = {
    val m = predictor.predict(data, false, treeLimit).head
    Vectors.dense(1 - m, m)
  }

  def predictRaw(data: FVec): Vector = {
    val m = predictor.predict(data, true, treeLimit).head
    Vectors.dense(- m, m)
  }

  override def rawToProbabilityInPlace(raw: Vector): Vector = {
    throw new Exception("XGBoost Classification model does not support \'rawToProbabilityInPlace\'")
  }
}

case class XGBoostPerformantClassificationModel(impl: XGBoostPerformantClassificationModelBase) extends ProbabilisticClassificationModel {
  override val numClasses: Int = impl.numClasses
  override val numFeatures: Int = impl.numFeatures
  def treeLimit: Int = impl.treeLimit

  def predictor: Predictor = impl.predictor

  def binaryClassificationModel: XGBoostPerformantBinaryClassificationModel = impl.asInstanceOf[XGBoostPerformantBinaryClassificationModel]
//  def multinomialClassificationModel: XGBoostMultinomialClassificationModel = impl.asInstanceOf[XGBoostMultinomialClassificationModel]

  def predict(data: FVec): Double = impl.predict(data)

  def predictLeaf(features: Vector): Seq[Double] = impl.predictLeaf(features)
  def predictLeaf(data: FVec): Seq[Double] = impl.predictLeaf(data)

  override def predictProbabilities(features: Vector): Vector = impl.predictProbabilities(features)
  def predictProbabilities(data: FVec): Vector = impl.predictProbabilities(data)

  override def predictRaw(features: Vector): Vector = impl.predictRaw(features)
  def predictRaw(data: FVec): Vector = impl.predictRaw(data)

  override def rawToProbabilityInPlace(raw: Vector): Vector = impl.rawToProbabilityInPlace(raw)

  override def outputSchema: StructType = StructType(
    "raw_prediction" -> TensorType.Double(numClasses),
//    "probability" -> TensorType.Double(numClasses),
    "prediction" -> ScalarType.Double.nonNullable
//    "leaf_prediction" -> ListType.Double,
//    "contrib_prediction" -> ListType.Double).get
  ).get
}
