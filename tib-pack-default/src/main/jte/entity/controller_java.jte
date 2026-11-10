@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.packageName()};

import jakarta.validation.Valid;
@if(entity.uuid())
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

/** The API of ${entity.plural()}. */
@RestController
@RequestMapping("${entity.path()}")
public class ${entity.name()}Controller {

  private final ${entity.name()}Service service;

  public ${entity.name()}Controller(${entity.name()}Service service) {
    this.service = service;
  }

  @if(entity.paginated())
  @GetMapping
  public Page<${entity.name()}Dto> list(Pageable pageable) {
    return service.list(pageable);
  }

  @else
  @GetMapping
  public List<${entity.name()}Dto> list() {
    return service.list();
  }

  @endif
  @GetMapping("/{id}")
  public ${entity.name()}Dto get(@PathVariable ${entity.idType()} id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ${entity.name()}Dto create(@Valid @RequestBody ${entity.name()}Dto dto) {
    return service.create(dto);
  }

  @PutMapping("/{id}")
  public ${entity.name()}Dto update(@PathVariable ${entity.idType()} id, @Valid @RequestBody ${entity.name()}Dto dto) {
    return service.update(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable ${entity.idType()} id) {
    service.delete(id);
  }
}
