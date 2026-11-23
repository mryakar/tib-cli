// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.web;

import jakarta.validation.Valid;
import me.yakar.cornerservice.item.application.ItemService;
import me.yakar.cornerservice.item.domain.Item;
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

/** The API of items: their DTOs, mapped to and from the aggregate. */
@RestController
@RequestMapping("/api/items")
public class ItemController {

  private final ItemService service;
  private final ItemMapper mapper;

  public ItemController(ItemService service, ItemMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping
  public Page<ItemDto> list(Pageable pageable) {
    return service.list(pageable).map(mapper::toDto);
  }

  @GetMapping("/{id}")
  public ItemDto get(@PathVariable Long id) {
    return mapper.toDto(service.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ItemDto create(@Valid @RequestBody ItemDto dto) {
    Item item = new Item();
    mapper.update(dto, item);
    return mapper.toDto(service.create(item));
  }

  @PutMapping("/{id}")
  public ItemDto update(@PathVariable Long id, @Valid @RequestBody ItemDto dto) {
    return mapper.toDto(service.update(id, item -> mapper.update(dto, item)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
