@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.packageName()};

@if(entity.uuidId())
import java.util.UUID;
@endif
@if(entity.paginated())
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@else
import java.util.List;
@endif
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of ${entity.plural()}. */
@Service
@Transactional
public class ${name}Service {

  private final ${name}Repository ${entity.plural()};
  private final ${name}Mapper mapper;

  public ${name}Service(${name}Repository ${entity.plural()}, ${name}Mapper mapper) {
    this.${entity.plural()} = ${entity.plural()};
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @if(entity.paginated())
  public Page<${name}Dto> list(Pageable pageable) {
    return ${entity.plural()}.findAll(pageable).map(mapper::toDto);
  }

  @else
  public List<${name}Dto> list() {
    return ${entity.plural()}.findAll(org.springframework.data.domain.Sort.by("${entity.idField()}")).stream().map(mapper::toDto).toList();
  }

  @endif
  @Transactional(readOnly = true)
  public ${name}Dto get(${entity.idType()} id) {
    return mapper.toDto(find(id));
  }

  public ${name}Dto create(${name}Dto dto) {
    ${name} ${entity.variable()} = new ${name}();
    mapper.update(dto, ${entity.variable()});
    return mapper.toDto(${entity.plural()}.saveAndFlush(${entity.variable()}));
  }

  public ${name}Dto update(${entity.idType()} id, ${name}Dto dto) {
    ${name} ${entity.variable()} = find(id);
    mapper.update(dto, ${entity.variable()});
    return mapper.toDto(${entity.plural()}.saveAndFlush(${entity.variable()}));
  }

  public void delete(${entity.idType()} id) {
    ${entity.plural()}.delete(find(id));
  }

  private ${name} find(${entity.idType()} id) {
    return ${entity.plural()}
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no ${entity.variable()} " + id));
  }
}
