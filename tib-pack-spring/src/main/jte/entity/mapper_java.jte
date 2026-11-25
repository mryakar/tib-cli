@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.Sources
@import me.yakar.tib.pack.spring.JpaModel
@param EntityModel entity
@param JpaModel jpa
!{String name = entity.name();}
package ${entity.webPackage()};

${Sources.importOf(entity.webPackage(), entity.domainPackage(), name)}

import org.springframework.stereotype.Component;
@if(!entity.refersById())
@for(var target : entity.otherTargets())
import ${target.packageName()}.${target.name()}Repository;
@endfor
@endif

/** Between the entity and its DTO@if(!entity.relations().isEmpty() && !entity.refersById()); a relation is resolved by the identifier of its target@elseif(!entity.relations().isEmpty()); a relation is the identifier of its target in both@endif. */
@Component
public class ${entity.webMapper()} {

  @if(!entity.targets().isEmpty() && !entity.refersById())
  @for(var target : entity.targets())
  private final ${target.name()}Repository ${target.plural()};
  @endfor

  public ${entity.webMapper()}(${jpa.repositoryParameters()}) {
    @for(var target : entity.targets())
    this.${target.plural()} = ${target.plural()};
    @endfor
  }

  @endif
  public ${name}Dto toDto(${name} ${entity.variable()}) {
    return new ${name}Dto(${entity.dtoArguments()});
  }

  /** Copies the DTO onto the entity, all but its identifier@if(entity.audited()) and its audit times@endif. */
  public void update(${name}Dto dto, ${name} ${entity.variable()}) {
    @for(var field : entity.fields())
    ${entity.variable()}.set${field.property()}(dto.${field.name()}());
    @endfor
    @for(var relation : entity.relations())
    @if(entity.refersById())
    ${entity.variable()}.set${relation.idProperty()}(dto.${relation.idField()}());
    @else
    ${entity.variable()}.set${relation.property()}(dto.${relation.idField()}() == null ? null : ${relation.target().plural()}.getReferenceById(dto.${relation.idField()}()));
    @endif
    @endfor
  }
}
