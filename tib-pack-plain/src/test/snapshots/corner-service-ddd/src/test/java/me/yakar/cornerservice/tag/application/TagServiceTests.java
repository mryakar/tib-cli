// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.tag.domain.Tag;
import me.yakar.cornerservice.tag.domain.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of tags, with the repository mocked. */
@ExtendWith(MockitoExtension.class)
class TagServiceTests {

  private static final Long ID = 1L;

  @Mock private Transactions transactions;
  @Mock private TagRepository repository;
  @InjectMocks private TagService service;

  /** The transactions just run the work, without a database. */
  @BeforeEach
  void runTheWork() {
    when(transactions.run(any()))
        .thenAnswer(call -> call.<Transactions.Work<?>>getArgument(0).run());
  }

  @Test
  void getsATag() throws SQLException {
    Tag tag = new Tag();
    when(repository.findById(ID)).thenReturn(Optional.of(tag));

    assertSame(tag, service.get(ID));
  }

  @Test
  void anUnknownTagIsNotFound() throws SQLException {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    NotFoundException e = assertThrows(NotFoundException.class, () -> service.get(ID));
    assertEquals("There is no tag " + ID, e.getMessage());
  }

  @Test
  void createsATag() throws SQLException {
    Tag tag = new Tag();
    when(repository.insert(tag)).thenReturn(tag);

    assertSame(tag, service.create(tag));
  }

  @Test
  void updatesATagAsTheChangeDoes() throws SQLException {
    Tag tag = new Tag();
    AtomicReference<Tag> changed = new AtomicReference<>();
    when(repository.findById(ID)).thenReturn(Optional.of(tag));

    assertSame(tag, service.update(ID, changed::set));
    assertSame(tag, changed.get());
    verify(repository).update(tag);
  }

  @Test
  void deletesATag() throws SQLException {
    Tag tag = new Tag();
    tag.setId(ID);
    when(repository.findById(ID)).thenReturn(Optional.of(tag));

    service.delete(ID);

    verify(repository).delete(ID);
  }
}
