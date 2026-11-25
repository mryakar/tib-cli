// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of items. */
@Service
@Transactional
public class ItemService {

  private final ItemRepository items;
  private final ItemMapper mapper;

  public ItemService(ItemRepository items, ItemMapper mapper) {
    this.items = items;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public Page<ItemDto> list(Pageable pageable) {
    return items.findAll(pageable).map(mapper::toDto);
  }

  @Transactional(readOnly = true)
  public ItemDto get(Long id) {
    return mapper.toDto(find(id));
  }

  public ItemDto create(ItemDto dto) {
    Item item = new Item();
    mapper.update(dto, item);
    return mapper.toDto(items.saveAndFlush(item));
  }

  public ItemDto update(Long id, ItemDto dto) {
    Item item = find(id);
    mapper.update(dto, item);
    return mapper.toDto(items.saveAndFlush(item));
  }

  public void delete(Long id) {
    items.delete(find(id));
  }

  private Item find(Long id) {
    return items
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no item " + id));
  }
}
