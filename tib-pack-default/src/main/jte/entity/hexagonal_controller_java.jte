@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
!{String mapper = entity.webMapper();}
package ${entity.webPackage()};

import ${entity.useCasePackage()}.${name}UseCase;
import ${entity.domainPackage()}.${name};
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The API of ${entity.plural()}: their DTOs, mapped to and from the domain, over the use cases. */
@RestController
@RequestMapping("${entity.path()}")
public class ${name}Controller {

  private final ${name}UseCase useCases;
  private final ${mapper} mapper;

  public ${name}Controller(${name}UseCase useCases, ${mapper} mapper) {
    this.useCases = useCases;
    this.mapper = mapper;
  }

  @GetMapping
  @if(entity.paginated())
  public Page<${name}Dto> list(Pageable pageable) {
    return useCases.list(pageable).map(mapper::toDto);
  }

  @else
  public List<${name}Dto> list() {
    return useCases.list().stream().map(mapper::toDto).toList();
  }

  @endif
  @GetMapping("/{id}")
  public ${name}Dto get(@PathVariable ${entity.idType()} id) {
    return mapper.toDto(useCases.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ${name}Dto create(@Valid @RequestBody ${name}Dto dto) {
    ${name} ${entity.variable()} = new ${name}();
    mapper.update(dto, ${entity.variable()});
    return mapper.toDto(useCases.create(${entity.variable()}));
  }

  @PutMapping("/{id}")
  public ${name}Dto update(@PathVariable ${entity.idType()} id, @Valid @RequestBody ${name}Dto dto) {
    return mapper.toDto(useCases.update(id, ${entity.variable()} -> mapper.update(dto, ${entity.variable()})));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable ${entity.idType()} id) {
    useCases.delete(id);
  }
}
