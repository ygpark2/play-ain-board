package modules

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import config.data.{Database, DevDatabase, ProdDatabase, TestDatabase}

class AppModule extends AbstractModule {
  def configure() = {
    bind(classOf[Database]).annotatedWith(Names.named("test")).to(classOf[TestDatabase])
    bind(classOf[Database]).annotatedWith(Names.named("development")).to(classOf[DevDatabase]).asEagerSingleton
    bind(classOf[Database]).annotatedWith(Names.named("production")).to(classOf[ProdDatabase])
  }
}
