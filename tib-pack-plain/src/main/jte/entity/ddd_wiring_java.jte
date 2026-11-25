@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.packageName()};

import ${entity.basePackage()}.Transactions;
import ${entity.applicationPackage()}.${name}Service;
import ${entity.persistencePackage()}.${name}Repository;
import ${entity.webPackage()}.${name}Handler;
import ${entity.webPackage()}.${name}Mapper;
import com.sun.net.httpserver.HttpHandler;

/**
 * The classes of ${entity.plural()}, wired by hand. {@code Routes} takes the handler of their API
 * from here; what a class of theirs takes in its constructor, it is given here.
 */
public final class ${name}Wiring {

  private ${name}Wiring() {}

  /** The handler of the API of ${entity.plural()}, on their service, which keeps them in the repository. */
  public static HttpHandler handler(Transactions transactions) {
    ${name}Repository repository = new ${name}Repository(transactions);
    ${name}Service service = new ${name}Service(transactions, repository);
    return new ${name}Handler(service, new ${name}Mapper());
  }
}
