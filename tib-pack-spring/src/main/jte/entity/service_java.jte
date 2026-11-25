@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.packageName()};

@if(entity.uuidId())
import java.util.UUID;
@endif
@if(entity.paginated())
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@else
import java.util.List;
import org.springframework.data.domain.Sort;
@endif
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of ${entity.plural()}. */
@Service
@Transactional
public class ${entity.name()}Service {

  private final ${entity.name()}Repository ${entity.plural()};
  private final ${entity.name()}Mapper mapper;

  public ${entity.name()}Service(${entity.name()}Repository ${entity.plural()}, ${entity.name()}Mapper mapper) {
    this.${entity.plural()} = ${entity.plural()};
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @if(entity.paginated())
  public Page<${entity.name()}Dto> list(Pageable pageable) {
    return ${entity.plural()}.findAll(pageable).map(mapper::toDto);
  }

  @else
  public List<${entity.name()}Dto> list() {
    return ${entity.plural()}.findAll(Sort.by("${entity.idField()}")).stream().map(mapper::toDto).toList();
  }

  @endif
  @Transactional(readOnly = true)
  public ${entity.name()}Dto get(${entity.idType()} id) {
    return mapper.toDto(find(id));
  }

  public ${entity.name()}Dto create(${entity.name()}Dto dto) {
    ${entity.name()} ${entity.variable()} = new ${entity.name()}();
    mapper.update(dto, ${entity.variable()});
    return mapper.toDto(${entity.plural()}.saveAndFlush(${entity.variable()}));
  }

  public ${entity.name()}Dto update(${entity.idType()} id, ${entity.name()}Dto dto) {
    ${entity.name()} ${entity.variable()} = find(id);
    mapper.update(dto, ${entity.variable()});
    return mapper.toDto(${entity.plural()}.saveAndFlush(${entity.variable()}));
  }

  public void delete(${entity.idType()} id) {
    ${entity.plural()}.delete(find(id));
  }

  private ${entity.name()} find(${entity.idType()} id) {
    return ${entity.plural()}
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no ${entity.variable()} " + id));
  }
}
