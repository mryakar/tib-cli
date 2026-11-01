@import gg.jte.support.ForSupport
@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
// Written by tib on every run. Do not edit: your changes would be lost.
package ${entity.packageName()};

import org.springframework.stereotype.Component;

/** Between the entity and its DTO. The relations are the service's to resolve. */
@Component
public class ${entity.name()}Mapper {

  public ${entity.name()}Dto toDto(${entity.name()} ${entity.variable()}) {
    return new ${entity.name()}Dto(
        @for(var argument : ForSupport.of(entity.dtoArguments()))
        ${argument.get()}${argument.isLast() ? ");" : ","}
        @endfor
  }

  /** Copies the fields of the DTO onto the entity; not its identifier, nor its relations. */
  public void update(${entity.name()}Dto dto, ${entity.name()} ${entity.variable()}) {
    @for(var field : entity.fields())
    ${entity.variable()}.set${field.property()}(dto.${field.name()}());
    @endfor
  }
}
