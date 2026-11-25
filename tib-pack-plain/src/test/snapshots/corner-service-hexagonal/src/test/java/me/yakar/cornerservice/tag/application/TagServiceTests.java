// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.application;

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
import me.yakar.cornerservice.tag.application.port.out.TagStore;
import me.yakar.cornerservice.tag.domain.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of tags, with the store mocked. */
@ExtendWith(MockitoExtension.class)
class TagServiceTests {

  private static final Long ID = 1L;

  @Mock private Transactions transactions;
  @Mock private TagStore store;
  @InjectMocks private TagService service;

  /** The transactions just run the work, without a database. */
  @BeforeEach
  void runTheWork() {
    when(transactions.run(any()))
        .thenAnswer(call -> call.<Transactions.Work<?>>getArgument(0).run());
  }

  @Test
  void getsATag() {
    Tag tag = new Tag();
    when(store.findById(ID)).thenReturn(Optional.of(tag));

    assertSame(tag, service.get(ID));
  }

  @Test
  void anUnknownTagIsNotFound() {
    when(store.findById(ID)).thenReturn(Optional.empty());

    NotFoundException e = assertThrows(NotFoundException.class, () -> service.get(ID));
    assertEquals("There is no tag " + ID, e.getMessage());
  }

  @Test
  void createsATag() {
    Tag tag = new Tag();
    when(store.insert(tag)).thenReturn(tag);

    assertSame(tag, service.create(tag));
  }

  @Test
  void updatesATagAsTheChangeDoes() {
    Tag tag = new Tag();
    AtomicReference<Tag> changed = new AtomicReference<>();
    when(store.findById(ID)).thenReturn(Optional.of(tag));

    assertSame(tag, service.update(ID, changed::set));
    assertSame(tag, changed.get());
    verify(store).update(tag);
  }

  @Test
  void deletesATag() {
    Tag tag = new Tag();
    tag.setId(ID);
    when(store.findById(ID)).thenReturn(Optional.of(tag));

    service.delete(ID);

    verify(store).delete(ID);
  }
}
