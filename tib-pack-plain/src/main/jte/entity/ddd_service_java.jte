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
import ${entity.domainPackage()}.${name};
import ${entity.domainPackage()}.${name}Repository;
import java.sql.SQLException;
@if(entity.uuidId())
import java.util.UUID;
@endif
import java.util.function.Consumer;

/**
 * The use cases of ${entity.plural()}, on the aggregate, each in a transaction; the web maps it to and
 * from its DTO.
 */
public class ${name}Service {

  private final Transactions transactions;
  private final ${name}Repository repository;

  public ${name}Service(Transactions transactions, ${name}Repository repository) {
    this.transactions = transactions;
    this.repository = repository;
  }

  @if(entity.paginated())
  /** The page of ${entity.plural()} with the number, from 0, of the size. */
  public Page<${name}> list(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size));
  }

  @else
  public List<${name}> list() {
    return transactions.run(repository::findAll);
  }

  @endif
  public ${name} get(${entity.idType()} id) {
    return transactions.run(() -> find(id));
  }

  public ${name} create(${name} ${entity.variable()}) {
    return transactions.run(() -> repository.insert(${entity.variable()}));
  }

  /** Changes the ${entity.variable()} as the change does, and saves it. */
  public ${name} update(${entity.idType()} id, Consumer<${name}> change) {
    return transactions.run(
        () -> {
          ${name} ${entity.variable()} = find(id);
          change.accept(${entity.variable()});
          repository.update(${entity.variable()});
          return ${entity.variable()};
        });
  }

  public void delete(${entity.idType()} id) {
    transactions.run(
        () -> {
          repository.delete(find(id).getId());
          return null;
        });
  }

  private ${name} find(${entity.idType()} id) throws SQLException {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("There is no ${entity.variable()} " + id));
  }
}
