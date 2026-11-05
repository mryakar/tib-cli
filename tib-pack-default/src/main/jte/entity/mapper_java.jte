@import gg.jte.support.ForSupport
@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
<%-- Whether the arguments of the DTO end with the fields: no relation and no audit times follow. --%>
!{boolean endsWithFields = entity.relations().isEmpty() && !entity.audited();}
package ${entity.packageName()};

${entity.mapperImports()}/** Between the entity and its DTO@if(!entity.relations().isEmpty()); a relation is resolved by the identifier of its target@endif. */
@Component
public class ${entity.name()}Mapper {

  @if(!entity.targets().isEmpty())
  @for(var target : entity.targets())
  private final ${target.name()}Repository ${target.plural()};
  @endfor<%-- keeps the blank line below --%>

  public ${entity.name()}Mapper(@for(var target : ForSupport.of(entity.targets()))${target.get().name()}Repository ${target.get().plural()}${target.isLast() ? "" : ", "}@endfor) {
    @for(var target : entity.targets())
    this.${target.plural()} = ${target.plural()};
    @endfor
  }

  @endif
  public ${entity.name()}Dto toDto(${entity.name()} ${entity.variable()}) {
    return new ${entity.name()}Dto(
        ${entity.variable()}.getId()${entity.fields().isEmpty() && endsWithFields ? ");" : ","}
        @for(var field : ForSupport.of(entity.fields()))
        ${entity.variable()}.get${field.get().property()}()${field.isLast() && endsWithFields ? ");" : ","}
        @endfor
        @for(var relation : ForSupport.of(entity.relations()))
        ${entity.variable()}.get${relation.get().property()}() == null ? null : ${entity.variable()}.get${relation.get().property()}().getId()${relation.isLast() && !entity.audited() ? ");" : ","}
        @endfor
        @if(entity.audited())
        ${entity.variable()}.getCreatedAt(),
        ${entity.variable()}.getUpdatedAt());
        @endif
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
