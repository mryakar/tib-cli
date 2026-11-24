@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.webPackage()};

${EntityModel.importOf(entity.webPackage(), entity.domainPackage(), entity.name())}

/** Between the entity and its DTO@if(!entity.relations().isEmpty()); a relation is the identifier of its target in both@endif. */
public class ${entity.name()}Mapper {

  public ${entity.name()}Dto toDto(${entity.name()} ${entity.variable()}) {
    return new ${entity.name()}Dto(${entity.dtoArguments()});
  }

  /** Copies the DTO onto the entity, all but its identifier@if(entity.audited()) and its audit times@endif. */
  public void update(${entity.name()}Dto dto, ${entity.name()} ${entity.variable()}) {
    @for(var field : entity.fields())
    ${entity.variable()}.set${field.property()}(dto.${field.name()}());
    @endfor
    @for(var relation : entity.relations())
    ${entity.variable()}.set${relation.idProperty()}(dto.${relation.idField()}());
    @endfor
  }
}
