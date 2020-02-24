package ml.combust.mleap.xgboost.runtime

import ml.combust.mleap.core.types._
import ml.combust.mleap.runtime.frame.{DefaultLeapFrame, Transformer}
import ml.combust.mleap.tensor.SparseTensor
import ml.combust.mleap.xgboost.runtime.testing.{BoosterUtils, BundleSerializationUtils, CachedDatasetUtils, FloatingPointApproximations}
import ml.dmlc.xgboost4j.scala.Booster
import org.scalatest.FunSpec
import XgbConverters._


class XGBoostRegressionModelParitySpec extends FunSpec
  with BoosterUtils
  with CachedDatasetUtils
  with BundleSerializationUtils
  with FloatingPointApproximations {

  def trainRegressor: Transformer = {

    val booster: Booster = trainBooster(binomialDataset)

    XGBoostRegression(
      "xgboostSingleThread",
      NodeShape.regression(),
      XGBoostRegressionModel(booster, numFeatures(leapFrameLibSVMtrain), 0)
    )
  }

  def equalityTestRowByRow(booster: Booster, mleapTransformer: Transformer, leapFrameDataset: DefaultLeapFrame) = {

    val featuresColumnIndex = leapFrameDataset.schema.indexOf("features").get

    leapFrameDataset.dataset.foreach {
      r=>
        val mleapResult = mleapTransformer.transform(DefaultLeapFrame(leapFrameDataset.schema, Seq(r))).get
        val mleapPredictionColIndex = mleapResult.schema.indexOf("prediction").get

        val singleRowDMatrix = r(featuresColumnIndex).asInstanceOf[SparseTensor[Double]].asXGB
        val boosterResult = booster.predict(singleRowDMatrix, false, 0).head(0)

        assert (boosterResult == mleapResult.dataset.head.getDouble(mleapPredictionColIndex))

    }
  }

  it("Results between the XGBoost4j booster and the MLeap Transformer are the same") {
    val booster = trainBooster(binomialDataset)
    val xgboostTransformer = trainRegressor

    val mleapBundle = serializeModelToMleapBundle(xgboostTransformer)
    val deserializedTransformer: Transformer = loadMleapTransformerFromBundle(mleapBundle)

    equalityTestRowByRow(booster, deserializedTransformer, leapFrameLibSVMtrain)
  }

  it("has the correct inputs and outputs with columns: prediction, probability and raw_prediction") {

    val transformer = trainRegressor
    val numFeatures = transformer.asInstanceOf[XGBoostRegression].model.numFeatures

    assert(transformer.schema.fields ==
      Seq(StructField("features", TensorType(BasicType.Double, Seq(numFeatures))),
        StructField("prediction", ScalarType.Double.nonNullable)))
  }

  it("Results are the same pre and post serialization") {
    val xgboostTransformer = trainRegressor

    val mleapBundle = serializeModelToMleapBundle(xgboostTransformer)
    val deserializedTransformer: Transformer = loadMleapTransformerFromBundle(mleapBundle)

    val preSerializationResult = xgboostTransformer.transform(leapFrameLibSVMtrain).get
    val deserializedModelResult = deserializedTransformer.transform(leapFrameLibSVMtrain).get

    assert(preSerializationResult.dataset == deserializedModelResult.dataset)
  }

  it("Test results are the same when using a dense dataset") {
    val xgboostTransformer = trainRegressor

    val mleapBundle = serializeModelToMleapBundle(xgboostTransformer)
    val deserializedTransformer: Transformer = loadMleapTransformerFromBundle(mleapBundle)

    val denseLeapFrame = toDenseFeaturesLeapFrame(leapFrameLibSVMtrain)

    val preSerializationResult = xgboostTransformer.transform(denseLeapFrame).get
    val deserializedResult = deserializedTransformer.transform(denseLeapFrame).get

    assert(preSerializationResult.dataset == deserializedResult.dataset)
  }
}
