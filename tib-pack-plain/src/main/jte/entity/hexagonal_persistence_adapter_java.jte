@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
!{String variable = entity.variable();}
package ${entity.persistencePackage()};

@if(entity.paginated())
import ${entity.basePackage()}.Page;
@else
import java.util.List;
@endif
import ${entity.basePackage()}.Transactions;
import ${entity.domainPackage()}.${name};
import ${entity.storePackage()}.${name}Store;
import java.util.Optional;
@if(entity.uuidId())
import java.util.UUID;
@endif

/**
 * The store of ${entity.plural()} on their table, through the repository, whose SQL follows the spec.
 * Every call joins the transaction of the use case that makes it, and a failure of the database
 * comes out of it as a {@link Transactions.DatabaseException}: the store knows no JDBC.
 */
public class ${name}PersistenceAdapter implements ${name}Store {

  private final Transactions transactions;
  private final ${name}Repository repository;

  public ${name}PersistenceAdapter(Transactions transactions, ${name}Repository repository) {
    this.transactions = transactions;
    this.repository = repository;
  }

  @if(entity.paginated())
  @Override
  public Page<${name}> findAll(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size));
  }
  @else
  @Override
  public List<${name}> findAll() {
    return transactions.run(repository::findAll);
  }
  @endif<%-- keeps the blank line below --%>

  @Override
  public Optional<${name}> findById(${entity.idType()} id) {
    return transactions.run(() -> repository.findById(id));
  }

  @Override
  public ${name} insert(${name} ${variable}) {
    return transactions.run(() -> repository.insert(${variable}));
  }

  @Override
  public void update(${name} ${variable}) {
    transactions.run(() -> repository.update(${variable}));
  }

  @Override
  public void delete(${entity.idType()} id) {
    transactions.run(() -> repository.delete(id));
  }
}
