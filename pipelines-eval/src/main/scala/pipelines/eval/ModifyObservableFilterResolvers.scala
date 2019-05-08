package pipelines.eval

import java.nio.file.Path

import pipelines.data.ModifyObservableResolver

object ModifyObservableFilterResolvers {
  def apply(): ModifyObservableResolver = ModifyObservableJsonFilter.Resolver().orElse(ModifyObservableAvroFilter.Resolver())
  def apply(dir : Path): ModifyObservableResolver = apply(ModifyObservableResolver.Default(dir))
  def apply(fallback : ModifyObservableResolver): ModifyObservableResolver = apply().orElse(fallback)
}
