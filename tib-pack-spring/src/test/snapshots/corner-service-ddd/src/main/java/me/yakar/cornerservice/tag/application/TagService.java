// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.application;

import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.tag.domain.Tag;
import me.yakar.cornerservice.tag.domain.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The use cases of tags, on the aggregate; the web maps it to and from its DTO. */
@Service
@Transactional
public class TagService {

  private final TagRepository tags;

  public TagService(TagRepository tags) {
    this.tags = tags;
  }

  @Transactional(readOnly = true)
  public Page<Tag> list(Pageable pageable) {
    return tags.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Tag get(Long id) {
    return find(id);
  }

  public Tag create(Tag tag) {
    return tags.saveAndFlush(tag);
  }

  /** Changes the tag as the change does, and saves it. */
  public Tag update(Long id, Consumer<Tag> change) {
    Tag tag = find(id);
    change.accept(tag);
    return tags.saveAndFlush(tag);
  }

  public void delete(Long id) {
    tags.delete(find(id));
  }

  private Tag find(Long id) {
    return tags.findById(id).orElseThrow(() -> new NotFoundException("There is no tag " + id));
  }
}
