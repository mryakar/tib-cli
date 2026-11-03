// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

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

/** The use cases of tags, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class TagServiceTests {

  private static final Long ID = 1L;

  @Mock private TagRepository tags;
  @Mock private TagMapper mapper;
  @InjectMocks private TagService service;

  @Test
  void getsATag() {
    Tag tag = new Tag();
    TagDto dto = dto();
    when(tags.findById(ID)).thenReturn(Optional.of(tag));
    when(mapper.toDto(tag)).thenReturn(dto);

    assertThat(service.get(ID)).isSameAs(dto);
  }

  @Test
  void anUnknownTagIsNotFound() {
    when(tags.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void createsATagFromItsDto() {
    TagDto dto = dto();
    when(tags.saveAndFlush(any(Tag.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(Tag.class))).thenReturn(dto);

    assertThat(service.create(dto)).isSameAs(dto);
    verify(mapper).update(eq(dto), any(Tag.class));
  }

  @Test
  void updatesATagFromItsDto() {
    Tag tag = new Tag();
    TagDto dto = dto();
    when(tags.findById(ID)).thenReturn(Optional.of(tag));
    when(tags.saveAndFlush(tag)).thenReturn(tag);
    when(mapper.toDto(tag)).thenReturn(dto);

    assertThat(service.update(ID, dto)).isSameAs(dto);
    verify(mapper).update(dto, tag);
  }

  @Test
  void deletesATag() {
    Tag tag = new Tag();
    when(tags.findById(ID)).thenReturn(Optional.of(tag));

    service.delete(ID);

    verify(tags).delete(tag);
  }

  private static TagDto dto() {
    return new TagDto(
        ID,
        "One",
        1L,
        1L);
  }
}
