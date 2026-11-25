// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

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

/** The API of items. */
@RestController
@RequestMapping("/api/items")
public class ItemController {

  private final ItemService service;

  public ItemController(ItemService service) {
    this.service = service;
  }

  @GetMapping
  public Page<ItemDto> list(Pageable pageable) {
    return service.list(pageable);
  }

  @GetMapping("/{id}")
  public ItemDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ItemDto create(@Valid @RequestBody ItemDto dto) {
    return service.create(dto);
  }

  @PutMapping("/{id}")
  public ItemDto update(@PathVariable Long id, @Valid @RequestBody ItemDto dto) {
    return service.update(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
