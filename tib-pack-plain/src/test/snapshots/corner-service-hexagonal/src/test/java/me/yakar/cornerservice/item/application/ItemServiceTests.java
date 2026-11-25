// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.item.application.port.out.ItemStore;
import me.yakar.cornerservice.item.domain.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of items, with the store mocked. */
@ExtendWith(MockitoExtension.class)
class ItemServiceTests {

  private static final Long ID = 1L;

  @Mock private Transactions transactions;
  @Mock private ItemStore store;
  @InjectMocks private ItemService service;

  /** The transactions just run the work, without a database. */
  @BeforeEach
  void runTheWork() {
    when(transactions.run(any()))
        .thenAnswer(call -> call.<Transactions.Work<?>>getArgument(0).run());
  }

  @Test
  void getsAnItem() {
    Item item = new Item();
    when(store.findById(ID)).thenReturn(Optional.of(item));

    assertSame(item, service.get(ID));
  }

  @Test
  void anUnknownItemIsNotFound() {
    when(store.findById(ID)).thenReturn(Optional.empty());

    NotFoundException e = assertThrows(NotFoundException.class, () -> service.get(ID));
    assertEquals("There is no item " + ID, e.getMessage());
  }

  @Test
  void createsAnItem() {
    Item item = new Item();
    when(store.insert(item)).thenReturn(item);

    assertSame(item, service.create(item));
  }

  @Test
  void updatesAnItemAsTheChangeDoes() {
    Item item = new Item();
    AtomicReference<Item> changed = new AtomicReference<>();
    when(store.findById(ID)).thenReturn(Optional.of(item));

    assertSame(item, service.update(ID, changed::set));
    assertSame(item, changed.get());
    verify(store).update(item);
  }

  @Test
  void deletesAnItem() {
    Item item = new Item();
    item.setId(ID);
    when(store.findById(ID)).thenReturn(Optional.of(item));

    service.delete(ID);

    verify(store).delete(ID);
  }
}
