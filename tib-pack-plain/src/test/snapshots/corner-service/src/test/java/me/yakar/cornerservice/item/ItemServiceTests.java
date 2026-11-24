// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Transactions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of items, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class ItemServiceTests {

  private static final Long ID = 1L;

  @Mock private Transactions transactions;
  @Mock private ItemRepository repository;
  @Mock private ItemMapper mapper;
  @InjectMocks private ItemService service;

  /** The transactions just run the work, without a database. */
  @BeforeEach
  void runTheWork() {
    when(transactions.run(any()))
        .thenAnswer(call -> call.<Transactions.Work<?>>getArgument(0).run());
  }

  @Test
  void getsAnItem() throws SQLException {
    Item item = new Item();
    ItemDto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(item));
    when(mapper.toDto(item)).thenReturn(dto);

    assertSame(dto, service.get(ID));
  }

  @Test
  void anUnknownItemIsNotFound() throws SQLException {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.get(ID));
  }

  @Test
  void createsAnItemFromItsDto() throws SQLException {
    ItemDto dto = dto();
    when(repository.insert(any(Item.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(Item.class))).thenReturn(dto);

    assertSame(dto, service.create(dto));
    verify(mapper).update(eq(dto), any(Item.class));
  }

  @Test
  void updatesAnItemFromItsDto() throws SQLException {
    Item item = new Item();
    ItemDto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(item));
    when(mapper.toDto(item)).thenReturn(dto);

    assertSame(dto, service.update(ID, dto));
    verify(mapper).update(dto, item);
    verify(repository).update(item);
  }

  @Test
  void deletesAnItem() throws SQLException {
    Item item = new Item();
    item.setId(ID);
    when(repository.findById(ID)).thenReturn(Optional.of(item));

    service.delete(ID);

    verify(repository).delete(ID);
  }

  private static ItemDto dto() {
    return ItemSamples.dto(1L).id(ID).build();
  }
}
