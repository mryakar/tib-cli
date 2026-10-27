// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

/** The API of tags. */
@RestController
@RequestMapping("/api/tags")
public class TagController {

  private final TagService service;

  public TagController(TagService service) {
    this.service = service;
  }

  @GetMapping
  public Page<TagDto> list(Pageable pageable) {
    return service.list(pageable);
  }

  @GetMapping("/{id}")
  public TagDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TagDto create(@Valid @RequestBody TagDto dto) {
    return service.create(dto);
  }

  @PutMapping("/{id}")
  public TagDto update(@PathVariable Long id, @Valid @RequestBody TagDto dto) {
    return service.update(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
