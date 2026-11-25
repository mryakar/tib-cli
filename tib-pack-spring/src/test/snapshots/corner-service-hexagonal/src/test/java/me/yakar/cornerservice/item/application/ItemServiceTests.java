// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.item.application.port.out.ItemStore;
import me.yakar.cornerservice.item.domain.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of items, with the store mocked. */
@ExtendWith(MockitoExtension.class)
class ItemServiceTests {

  private static final Long ID = 1L;

  @Mock private ItemStore items;
  @InjectMocks private ItemService service;

  @Test
  void getsAnItem() {
    Item item = new Item();
    when(items.findById(ID)).thenReturn(Optional.of(item));

    assertThat(service.get(ID)).isSameAs(item);
  }

  @Test
  void anUnknownItemIsNotFound() {
    when(items.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("There is no item " + ID);
  }

  @Test
  void createsAnItem() {
    Item item = new Item();
    when(items.save(item)).thenReturn(item);

    assertThat(service.create(item)).isSameAs(item);
  }

  @Test
  void updatesAnItemAsTheChangeDoes() {
    Item item = new Item();
    AtomicReference<Item> changed = new AtomicReference<>();
    when(items.findById(ID)).thenReturn(Optional.of(item));
    when(items.save(item)).thenReturn(item);

    assertThat(service.update(ID, changed::set)).isSameAs(item);
    assertThat(changed.get()).isSameAs(item);
  }

  @Test
  void deletesAnItem() {
    Item item = new Item();
    when(items.findById(ID)).thenReturn(Optional.of(item));

    service.delete(ID);

    verify(items).delete(item);
  }
}
