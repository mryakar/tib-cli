@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.applicationPackage()};

import ${entity.basePackage()}.NotFoundException;
@if(entity.paginated())
import ${entity.basePackage()}.Page;
@else
import java.util.List;
@endif
import ${entity.basePackage()}.Transactions;
import ${entity.useCasePackage()}.${name}UseCase;
import ${entity.storePackage()}.${name}Store;
import ${entity.domainPackage()}.${name};
@if(entity.uuidId())
import java.util.UUID;
@endif
import java.util.function.Consumer;

/**
 * The use cases of ${entity.plural()}, on the domain, which the store keeps, each in a transaction; the
 * adapters know the rest.
 */
public class ${name}Service implements ${name}UseCase {

  private final Transactions transactions;
  private final ${name}Store store;

  public ${name}Service(Transactions transactions, ${name}Store store) {
    this.transactions = transactions;
    this.store = store;
  }

  @if(entity.paginated())
  @Override
  public Page<${name}> list(int page, int size) {
    return transactions.run(() -> store.findAll(page, size));
  }
  @else
  @Override
  public List<${name}> list() {
    return transactions.run(store::findAll);
  }
  @endif

  @Override
  public ${name} get(${entity.idType()} id) {
    return transactions.run(() -> find(id));
  }

  @Override
  public ${name} create(${name} ${entity.variable()}) {
    return transactions.run(() -> store.insert(${entity.variable()}));
  }

  @Override
  public ${name} update(${entity.idType()} id, Consumer<${name}> change) {
    return transactions.run(
        () -> {
          ${name} ${entity.variable()} = find(id);
          change.accept(${entity.variable()});
          if (!store.update(${entity.variable()})) {
            throw notFound(id);
          }
          return ${entity.variable()};
        });
  }

  @Override
  public void delete(${entity.idType()} id) {
    transactions.run(
        () -> {
          if (!store.delete(id)) {
            throw notFound(id);
          }
          return null;
        });
  }

  private ${name} find(${entity.idType()} id) {
    return store.findById(id).orElseThrow(() -> notFound(id));
  }

  private static NotFoundException notFound(${entity.idType()} id) {
    return new NotFoundException("There is no ${entity.variable()} " + id);
  }
}
