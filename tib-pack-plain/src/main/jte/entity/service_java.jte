@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.packageName()};

import ${entity.basePackage()}.NotFoundException;
@if(entity.paginated())
import ${entity.basePackage()}.Page;
@else
import java.util.List;
@endif
import ${entity.basePackage()}.Transactions;
import java.sql.SQLException;
@if(entity.uuidId())
import java.util.UUID;
@endif

/** The use cases of ${entity.plural()}, each in a transaction, on their DTOs. */
public class ${name}Service {

  private final Transactions transactions;
  private final ${name}Repository repository;
  private final ${name}Mapper mapper;

  public ${name}Service(Transactions transactions, ${name}Repository repository, ${name}Mapper mapper) {
    this.transactions = transactions;
    this.repository = repository;
    this.mapper = mapper;
  }

  @if(entity.paginated())
  /** The page of ${entity.plural()} with the number, from 0, of the size. */
  public Page<${name}Dto> list(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size).map(mapper::toDto));
  }

  @else
  public List<${name}Dto> list() {
    return transactions.run(() -> repository.findAll().stream().map(mapper::toDto).toList());
  }

  @endif
  public ${name}Dto get(${entity.idType()} id) {
    return transactions.run(() -> mapper.toDto(find(id)));
  }

  public ${name}Dto create(${name}Dto dto) {
    return transactions.run(
        () -> {
          ${name} ${entity.variable()} = new ${name}();
          mapper.update(dto, ${entity.variable()});
          return mapper.toDto(repository.insert(${entity.variable()}));
        });
  }

  public ${name}Dto update(${entity.idType()} id, ${name}Dto dto) {
    return transactions.run(
        () -> {
          ${name} ${entity.variable()} = find(id);
          mapper.update(dto, ${entity.variable()});
          repository.update(${entity.variable()});
          return mapper.toDto(${entity.variable()});
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
