// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

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

/** The use cases of tags, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class TagServiceTests {

  private static final Long ID = 1L;

  @Mock private Transactions transactions;
  @Mock private TagRepository repository;
  @Mock private TagMapper mapper;
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
    TagDto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(tag));
    when(mapper.toDto(tag)).thenReturn(dto);

    assertSame(dto, service.get(ID));
  }

  @Test
  void anUnknownTagIsNotFound() throws SQLException {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.get(ID));
  }

  @Test
  void createsATagFromItsDto() throws SQLException {
    TagDto dto = dto();
    when(repository.insert(any(Tag.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(Tag.class))).thenReturn(dto);

    assertSame(dto, service.create(dto));
    verify(mapper).update(eq(dto), any(Tag.class));
  }

  @Test
  void updatesATagFromItsDto() throws SQLException {
    Tag tag = new Tag();
    TagDto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(tag));
    when(mapper.toDto(tag)).thenReturn(dto);

    assertSame(dto, service.update(ID, dto));
    verify(mapper).update(dto, tag);
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

  private static TagDto dto() {
    return TagSamples.dto(1L).id(ID).build();
  }
}
