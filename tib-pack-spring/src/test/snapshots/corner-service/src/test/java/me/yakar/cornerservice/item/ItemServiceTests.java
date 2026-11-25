// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of items, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class ItemServiceTests {

  private static final Long ID = 1L;

  @Mock private ItemRepository items;
  @Mock private ItemMapper mapper;
  @InjectMocks private ItemService service;

  @Test
  void getsAnItem() {
    Item item = new Item();
    ItemDto dto = dto();
    when(items.findById(ID)).thenReturn(Optional.of(item));
    when(mapper.toDto(item)).thenReturn(dto);

    assertThat(service.get(ID)).isSameAs(dto);
  }

  @Test
  void anUnknownItemIsNotFound() {
    when(items.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void createsAnItemFromItsDto() {
    ItemDto dto = dto();
    when(items.saveAndFlush(any(Item.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(Item.class))).thenReturn(dto);

    assertThat(service.create(dto)).isSameAs(dto);
    verify(mapper).update(eq(dto), any(Item.class));
  }

  @Test
  void updatesAnItemFromItsDto() {
    Item item = new Item();
    ItemDto dto = dto();
    when(items.findById(ID)).thenReturn(Optional.of(item));
    when(items.saveAndFlush(item)).thenReturn(item);
    when(mapper.toDto(item)).thenReturn(dto);

    assertThat(service.update(ID, dto)).isSameAs(dto);
    verify(mapper).update(dto, item);
  }

  @Test
  void deletesAnItem() {
    Item item = new Item();
    when(items.findById(ID)).thenReturn(Optional.of(item));

    service.delete(ID);

    verify(items).delete(item);
  }

  private static ItemDto dto() {
    return ItemSamples.dto(1L).id(ID).build();
  }
}
