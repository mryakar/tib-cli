// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.adapter.in.web;

import jakarta.validation.Valid;
import me.yakar.cornerservice.tag.application.port.in.TagUseCase;
import me.yakar.cornerservice.tag.domain.Tag;
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

/** The API of tags: their DTOs, mapped to and from the domain, over the use cases. */
@RestController
@RequestMapping("/api/tags")
public class TagController {

  private final TagUseCase useCases;
  private final TagWebMapper mapper;

  public TagController(TagUseCase useCases, TagWebMapper mapper) {
    this.useCases = useCases;
    this.mapper = mapper;
  }

  @GetMapping
  public Page<TagDto> list(Pageable pageable) {
    return useCases.list(pageable).map(mapper::toDto);
  }

  @GetMapping("/{id}")
  public TagDto get(@PathVariable Long id) {
    return mapper.toDto(useCases.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TagDto create(@Valid @RequestBody TagDto dto) {
    Tag tag = new Tag();
    mapper.update(dto, tag);
    return mapper.toDto(useCases.create(tag));
  }

  @PutMapping("/{id}")
  public TagDto update(@PathVariable Long id, @Valid @RequestBody TagDto dto) {
    return mapper.toDto(useCases.update(id, tag -> mapper.update(dto, tag)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    useCases.delete(id);
  }
}
