@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.applicationPackage()};

import ${entity.basePackage()}.NotFoundException;
import ${entity.domainPackage()}.${name};
import ${entity.domainPackage()}.${name}Repository;
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

/** The use cases of ${entity.plural()}, on the aggregate; the web maps it to and from its DTO. */
@Service
@Transactional
public class ${name}Service {

  private final ${name}Repository ${entity.plural()};

  public ${name}Service(${name}Repository ${entity.plural()}) {
    this.${entity.plural()} = ${entity.plural()};
  }

  @Transactional(readOnly = true)
  @if(entity.paginated())
  public Page<${name}> list(Pageable pageable) {
    return ${entity.plural()}.findAll(pageable);
  }

  @else
  public List<${name}> list() {
    return ${entity.plural()}.findAll(org.springframework.data.domain.Sort.by("${entity.idField()}"));
  }

  @endif
  @Transactional(readOnly = true)
  public ${name} get(${entity.idType()} id) {
    return find(id);
  }

  public ${name} create(${name} ${entity.variable()}) {
    return ${entity.plural()}.saveAndFlush(${entity.variable()});
  }

  /** Changes the ${entity.variable()} as the change does, and saves it. */
  public ${name} update(${entity.idType()} id, Consumer<${name}> change) {
    ${name} ${entity.variable()} = find(id);
    change.accept(${entity.variable()});
    return ${entity.plural()}.saveAndFlush(${entity.variable()});
  }

  public void delete(${entity.idType()} id) {
    ${entity.plural()}.delete(find(id));
  }

  private ${name} find(${entity.idType()} id) {
    return ${entity.plural()}
        .findById(id)
        .orElseThrow(() -> new NotFoundException("There is no ${entity.variable()} " + id));
  }
}
