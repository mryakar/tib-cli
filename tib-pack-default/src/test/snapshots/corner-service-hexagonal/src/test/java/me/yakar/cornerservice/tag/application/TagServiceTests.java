// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.tag.application.port.out.TagStore;
import me.yakar.cornerservice.tag.domain.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of tags, with the store mocked. */
@ExtendWith(MockitoExtension.class)
class TagServiceTests {

  private static final Long ID = 1L;

  @Mock private TagStore tags;
  @InjectMocks private TagService service;

  @Test
  void getsATag() {
    Tag tag = new Tag();
    when(tags.findById(ID)).thenReturn(Optional.of(tag));

    assertThat(service.get(ID)).isSameAs(tag);
  }

  @Test
  void anUnknownTagIsNotFound() {
    when(tags.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("There is no tag " + ID);
  }

  @Test
  void createsATag() {
    Tag tag = new Tag();
    when(tags.save(tag)).thenReturn(tag);

    assertThat(service.create(tag)).isSameAs(tag);
  }

  @Test
  void updatesATagAsTheChangeDoes() {
    Tag tag = new Tag();
    AtomicReference<Tag> changed = new AtomicReference<>();
    when(tags.findById(ID)).thenReturn(Optional.of(tag));
    when(tags.save(tag)).thenReturn(tag);

    assertThat(service.update(ID, changed::set)).isSameAs(tag);
    assertThat(changed.get()).isSameAs(tag);
  }

  @Test
  void deletesATag() {
    Tag tag = new Tag();
    when(tags.findById(ID)).thenReturn(Optional.of(tag));

    service.delete(ID);

    verify(tags).delete(tag);
  }
}
