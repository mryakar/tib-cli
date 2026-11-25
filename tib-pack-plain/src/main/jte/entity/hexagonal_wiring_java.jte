@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.packageName()};

import ${entity.basePackage()}.Transactions;
import ${entity.webPackage()}.${name}Handler;
import ${entity.webPackage()}.${name}WebMapper;
import ${entity.persistencePackage()}.${name}PersistenceAdapter;
import ${entity.persistencePackage()}.${name}Repository;
import ${entity.applicationPackage()}.${name}Service;
import ${entity.storePackage()}.${name}Store;
import com.sun.net.httpserver.HttpHandler;

/**
 * The classes of ${entity.plural()}, wired by hand. {@code Routes} takes the handler of their API
 * from here; what a class of theirs takes in its constructor, it is given here.
 */
public final class ${name}Wiring {

  private ${name}Wiring() {}

  /** The handler of the API of ${entity.plural()}, on their use cases, which keep them in the store. */
  public static HttpHandler handler(Transactions transactions) {
    ${name}Repository repository = new ${name}Repository(transactions);
    ${name}Store store = new ${name}PersistenceAdapter(transactions, repository);
    ${name}Service service = new ${name}Service(transactions, store);
    return new ${name}Handler(service, new ${name}WebMapper());
  }
}
