@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.applicationPackage()};

import ${entity.basePackage()}.NotFoundException;
import ${entity.domainPackage()}.${name};
import ${entity.useCasePackage()}.${name}UseCase;
import ${entity.storePackage()}.${name}Store;
@if(entity.uuidId())
import java.util.UUID;
@endif
import java.util.function.Consumer;
@if(entity.paginated())
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@else
import java.util.List;
@endif
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The use cases of ${entity.plural()}, on the domain, which the store keeps; the adapters know the rest. */
@Service
@Transactional
public class ${name}Service implements ${name}UseCase {

  private final ${name}Store ${entity.plural()};

  public ${name}Service(${name}Store ${entity.plural()}) {
    this.${entity.plural()} = ${entity.plural()};
  }

  @Override
  @Transactional(readOnly = true)
  @if(entity.paginated())
  public Page<${name}> list(Pageable pageable) {
    return ${entity.plural()}.findAll(pageable);
  }

  @else
  public List<${name}> list() {
    return ${entity.plural()}.findAll();
  }

  @endif
  @Override
  @Transactional(readOnly = true)
  public ${name} get(${entity.idType()} id) {
    return find(id);
  }

  @Override
  public ${name} create(${name} ${entity.variable()}) {
    return ${entity.plural()}.save(${entity.variable()});
  }

  @Override
  public ${name} update(${entity.idType()} id, Consumer<${name}> change) {
    ${name} ${entity.variable()} = find(id);
    change.accept(${entity.variable()});
    return ${entity.plural()}.save(${entity.variable()});
  }

  @Override
  public void delete(${entity.idType()} id) {
    ${entity.plural()}.delete(find(id));
  }

  private ${name} find(${entity.idType()} id) {
    return ${entity.plural()}
        .findById(id)
        .orElseThrow(() -> new NotFoundException("There is no ${entity.variable()} " + id));
  }
}
