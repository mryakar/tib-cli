// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.adapter.out.persistence;

import java.util.Optional;
import me.yakar.cornerservice.tag.application.port.out.TagStore;
import me.yakar.cornerservice.tag.domain.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** The store of tags on their table, through JPA; the mapper maps them to and from the domain. */
@Component
public class TagPersistenceAdapter implements TagStore {

  private final TagJpaRepository tags;
  private final TagPersistenceMapper mapper;

  public TagPersistenceAdapter(TagJpaRepository tags, TagPersistenceMapper mapper) {
    this.tags = tags;
    this.mapper = mapper;
  }

  @Override
  public Page<Tag> findAll(Pageable pageable) {
    return tags.findAll(pageable).map(mapper::toDomain);
  }

  @Override
  public Optional<Tag> findById(Long id) {
    return tags.findById(id).map(mapper::toDomain);
  }

  /** Saves the tag and flushes it, so that a broken constraint shows here. */
  @Override
  public Tag save(Tag tag) {
    return mapper.toDomain(tags.saveAndFlush(mapper.toEntity(tag)));
  }

  @Override
  public void delete(Tag tag) {
    tags.deleteById(tag.getId());
  }
}
