@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.packageName()};

import org.springframework.stereotype.Component;
@for(var target : entity.otherTargets())
import ${target.packageName()}.${target.name()}Repository;
@endfor

/** Between the entity and its DTO@if(!entity.relations().isEmpty()); a relation is resolved by the identifier of its target@endif. */
@Component
public class ${entity.name()}Mapper {

  @if(!entity.targets().isEmpty())
  @for(var target : entity.targets())
  private final ${target.name()}Repository ${target.plural()};
  @endfor<%-- keeps the blank line below --%>

  public ${entity.name()}Mapper(${entity.repositoryParameters()}) {
    @for(var target : entity.targets())
    this.${target.plural()} = ${target.plural()};
    @endfor
  }

  @endif
  public ${entity.name()}Dto toDto(${entity.name()} ${entity.variable()}) {
    return new ${entity.name()}Dto(${entity.dtoArguments()});
  }

  /** Copies the DTO onto the entity, all but its identifier@if(entity.audited()) and its audit times@endif. */
  public void update(${entity.name()}Dto dto, ${entity.name()} ${entity.variable()}) {
    @for(var field : entity.fields())
    ${entity.variable()}.set${field.property()}(dto.${field.name()}());
    @endfor
    @for(var relation : entity.relations())
    ${entity.variable()}.set${relation.property()}(dto.${relation.idField()}() == null ? null : ${relation.target().plural()}.getReferenceById(dto.${relation.idField()}()));
    @endfor
  }
}
