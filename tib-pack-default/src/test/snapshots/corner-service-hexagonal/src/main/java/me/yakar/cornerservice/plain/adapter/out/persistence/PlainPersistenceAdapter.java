// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.plain.adapter.out.persistence;

import java.util.Optional;
import me.yakar.cornerservice.plain.application.port.out.PlainStore;
import me.yakar.cornerservice.plain.domain.Plain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** The store of plains on their table, through JPA; the mapper maps them to and from the domain. */
@Component
public class PlainPersistenceAdapter implements PlainStore {

  private final PlainJpaRepository plains;
  private final PlainPersistenceMapper mapper;

  public PlainPersistenceAdapter(PlainJpaRepository plains, PlainPersistenceMapper mapper) {
    this.plains = plains;
    this.mapper = mapper;
  }

  @Override
  public Page<Plain> findAll(Pageable pageable) {
    return plains.findAll(pageable).map(mapper::toDomain);
  }

  @Override
  public Optional<Plain> findById(Long id) {
    return plains.findById(id).map(mapper::toDomain);
  }

  /** Saves the plain and flushes it, so that a broken constraint shows here. */
  @Override
  public Plain save(Plain plain) {
    return mapper.toDomain(plains.saveAndFlush(mapper.toEntity(plain)));
  }

  @Override
  public void delete(Plain plain) {
    plains.deleteById(plain.getId());
  }
}
