// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.application;

import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.tag.application.port.in.TagUseCase;
import me.yakar.cornerservice.tag.application.port.out.TagStore;
import me.yakar.cornerservice.tag.domain.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The use cases of tags, on the domain, which the store keeps; the adapters know the rest. */
@Service
@Transactional
public class TagService implements TagUseCase {

  private final TagStore tags;

  public TagService(TagStore tags) {
    this.tags = tags;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Tag> list(Pageable pageable) {
    return tags.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Tag get(Long id) {
    return find(id);
  }

  @Override
  public Tag create(Tag tag) {
    return tags.save(tag);
  }

  @Override
  public Tag update(Long id, Consumer<Tag> change) {
    Tag tag = find(id);
    change.accept(tag);
    return tags.save(tag);
  }

  @Override
  public void delete(Long id) {
    tags.delete(find(id));
  }

  private Tag find(Long id) {
    return tags.findById(id).orElseThrow(() -> new NotFoundException("There is no tag " + id));
  }
}
