@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.spring.JpaModel
@param EntityModel entity
@param JpaModel jpa
!{String name = entity.name();}
!{String plural = entity.plural();}
package ${entity.persistencePackage()};

import ${entity.domainPackage()}.${name};
import ${entity.storePackage()}.${name}Store;
import java.util.Optional;
@if(entity.uuidId())
import java.util.UUID;
@endif
@if(entity.paginated())
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@else
import java.util.List;
@endif
import org.springframework.stereotype.Component;

/** The store of ${plural} on their table, through JPA; the mapper maps them to and from the domain. */
@Component
public class ${name}PersistenceAdapter implements ${name}Store {

  private final ${jpa.repository()} ${plural};
  private final ${name}PersistenceMapper mapper;

  public ${name}PersistenceAdapter(${jpa.repository()} ${plural}, ${name}PersistenceMapper mapper) {
    this.${plural} = ${plural};
    this.mapper = mapper;
  }

  @Override
  @if(entity.paginated())
  public Page<${name}> findAll(Pageable pageable) {
    return ${plural}.findAll(pageable).map(mapper::toDomain);
  }
  @else
  public List<${name}> findAll() {
    return ${plural}.findAll().stream().map(mapper::toDomain).toList();
  }
  @endif

  @Override
  public Optional<${name}> findById(${entity.idType()} id) {
    return ${plural}.findById(id).map(mapper::toDomain);
  }

  /** Saves the ${entity.variable()} and flushes it, so that a broken constraint shows here. */
  @Override
  public ${name} save(${name} ${entity.variable()}) {
    return mapper.toDomain(${plural}.saveAndFlush(mapper.toEntity(${entity.variable()})));
  }

  @Override
  public void delete(${name} ${entity.variable()}) {
    ${plural}.deleteById(${entity.variable()}.getId());
  }
}
