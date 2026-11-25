@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.Sources
@param EntityModel entity
!{String name = entity.name();}
package ${entity.webPackage()};

${Sources.importOf(entity.webPackage(), entity.domainPackage(), name)}

/** Between the entity and its DTO@if(!entity.relations().isEmpty()); a relation is the identifier of its target in both@endif. */
public class ${entity.webMapper()} {

  public ${name}Dto toDto(${name} ${entity.variable()}) {
    return new ${name}Dto(${entity.dtoArguments()});
  }

  /** Copies the DTO onto the entity, all but its identifier@if(entity.audited()) and its audit times@endif. */
  public void update(${name}Dto dto, ${name} ${entity.variable()}) {
    @for(var field : entity.fields())
    ${entity.variable()}.set${field.property()}(dto.${field.name()}());
    @endfor
    @for(var relation : entity.relations())
    ${entity.variable()}.set${relation.idProperty()}(dto.${relation.idField()}());
    @endfor
  }
}
