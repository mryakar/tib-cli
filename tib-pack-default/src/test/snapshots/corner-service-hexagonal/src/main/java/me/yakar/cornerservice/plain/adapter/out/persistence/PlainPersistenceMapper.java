// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.plain.adapter.out.persistence;

import me.yakar.cornerservice.plain.domain.Plain;
import org.springframework.stereotype.Component;

/** Between the table and the domain: every value, the identifier included. */
@Component
public class PlainPersistenceMapper {

  public Plain toDomain(PlainEntity entity) {
    Plain plain = new Plain();
    plain.setId(entity.getId());
    plain.setLabel(entity.getLabel());
    return plain;
  }

  public PlainEntity toEntity(Plain plain) {
    PlainEntity entity = new PlainEntity();
    entity.setId(plain.getId());
    entity.setLabel(plain.getLabel());
    return entity;
  }
}
