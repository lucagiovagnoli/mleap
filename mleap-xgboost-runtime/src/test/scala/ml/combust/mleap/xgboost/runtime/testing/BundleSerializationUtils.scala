package ml.combust.mleap.xgboost.runtime.testing

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

import ml.combust.bundle.BundleFile
import ml.combust.bundle.serializer.SerializationFormat
import ml.combust.mleap.runtime.{MleapContext, frame}
import ml.combust.mleap.runtime.frame.Transformer
import resource.managed


trait BundleSerializationUtils {

  def serializeModelToMleapBundle(transformer: Transformer): File = {
    import ml.combust.mleap.runtime.MleapSupport._

    val temporaryDir = Files.createTempDirectory("xgboost-runtime-parity").toString
    val path = Paths.get(temporaryDir, s"${this.getClass.getName}.zip")
    val file = new File(path.toUri)
    file.delete()

    for(bf <- managed(BundleFile(file))) {
      transformer.writeBundle.format(SerializationFormat.Json).save(bf).get
    }
    file
  }

  def loadMleapTransformerFromBundle(bundleFile: File)
                                    (implicit context: MleapContext): frame.Transformer = {

    import ml.combust.mleap.runtime.MleapSupport._

    (for(bf <- managed(BundleFile(bundleFile))) yield {
      bf.loadMleapBundle().get.root
    }).tried.get
  }

}
