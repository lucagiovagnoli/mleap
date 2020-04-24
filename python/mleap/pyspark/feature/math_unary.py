from enum import Enum

from pyspark import keyword_only
from pyspark.ml.param.shared import HasInputCol, HasOutputCol
from pyspark.ml.util import JavaMLReadable
from pyspark.ml.util import JavaMLWritable
from pyspark.ml.util import _jvm
from pyspark.ml.wrapper import JavaTransformer

from mleap.pyspark.py2scala import jvm_scala_object


class UnaryOperation(Enum):
    Sin = 1
    Cos = 2
    Tan = 3
    Log = 4
    Exp = 5
    Abs = 6
    Sqrt = 7


class MathUnary(JavaTransformer, HasInputCol, HasOutputCol, JavaMLReadable, JavaMLWritable):

    @keyword_only
    def __init__(self, operation=None, inputCol=None, outputCol=None):
        """
        Computes the mathematical unary `operation` over the input column.

        NOTE: we can't make `operation` a JavaParam (as in pyspark) because the
            underlying scala object MathUnary uses a MathUnaryModel to store
            the info about the unary operation (sin, tan, etc.)

            If operation is a JavaParam, py4j will fail trying to set it on the
            underlying scala object.

            If operation doesn't have a default value, then pyspark will fail
            upon deserialization trying to instantiate this object without args:
                (it just runs py_type() where py_type is the class name)

        """
        super(MathUnary, self).__init__()

        # if operation=None, it means that pyspark is reloading the model
        # from disk and calling this method without args. In such case we don't
        # need to set _java_obj here because pyspark will set it after creation
        #
        # if operation is not None, we can proceed to instantiate the scala classes
        if operation:
            scalaUnaryOperation = jvm_scala_object(
                _jvm().ml.combust.mleap.core.feature.UnaryOperation,
                operation.name
            )

            scalaMathUnaryModel = _jvm().ml.combust.mleap.core.feature.MathUnaryModel(scalaUnaryOperation)

            self._java_obj = self._new_java_obj(
                "org.apache.spark.ml.mleap.feature.MathUnary",
                self.uid,
                scalaMathUnaryModel,
            )

        self._setDefault()
        self.setParams(inputCol=inputCol, outputCol=outputCol)

    @keyword_only
    def setParams(self, inputCol=None, outputCol=None):
        """
        Sets params for this MathUnary.
        """
        kwargs = self._input_kwargs
        return self._set(**kwargs)

    def setInputCol(self, value):
        """
        Sets the value of :py:attr:`inputCol`.
        """
        return self._set(inputCol=value)

    def setOutputCol(self, value):
        """
        Sets the value of :py:attr:`outputCol`.
        """
        return self._set(outputCol=value)