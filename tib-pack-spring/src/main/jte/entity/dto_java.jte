@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.spring.DtoModel
@param EntityModel entity
@param DtoModel dto
package ${entity.webPackage()};

@for(var field : entity.fields())
@if(!field.nullable())
import jakarta.validation.constraints.NotNull;
@endif
@if(field.length() != null)
import jakarta.validation.constraints.Size;
@endif
${field.typeImport()}
@endfor
@for(var relation : entity.relations())
@if(!relation.nullable())
import jakarta.validation.constraints.NotNull;
@endif
@if(relation.target().uuidId())
import java.util.UUID;
@endif
@endfor
@if(entity.uuidId())
import java.util.UUID;
@endif
@if(entity.audited())
import java.time.Instant;
@endif

/**
 * ${entity.article()} ${entity.variable()} as the API shows and takes it@if(!entity.relations().isEmpty()); a relation is the identifier of its target@endif.
 @if(entity.audited())
 * The identifier and the audit times are read only; the service ignores them.
 @else
 * The identifier is read only; the service ignores it.
 @endif
 */
public record ${entity.name()}Dto(${dto.components()}) {}
