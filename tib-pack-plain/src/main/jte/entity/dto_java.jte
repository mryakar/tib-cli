@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.plain.DtoModel
@param EntityModel entity
@param DtoModel dto
package ${entity.webPackage()};

@if(entity.audited())
import java.time.Instant;
@endif
@if(!dto.checks().isEmpty())
import java.util.ArrayList;
@endif
import java.util.List;
@if(entity.uuidId())
import java.util.UUID;
@endif
@for(var field : entity.fields())
${field.typeImport()}
@endfor
@for(var relation : entity.relations())
@if(relation.target().uuidId())
import java.util.UUID;
@endif
@endfor

/**
 * ${entity.article()} ${entity.variable()} as the API shows and takes it@if(!entity.relations().isEmpty()); a relation is the identifier of its target@endif.
 @if(entity.audited())
 * The identifier and the audit times are read only; the service ignores them.
 @else
 * The identifier is read only; the service ignores it.
 @endif
 */
public record ${entity.name()}Dto(${dto.components()}) {

  /** The rules of the spec that the values break, each as the value and the rule; none when they keep them all. */
  public List<String> problems() {
    @if(dto.checks().isEmpty())
    return List.of();
    @else
    List<String> problems = new ArrayList<>();
    @for(var check : dto.checks())
    @if(check.required() && check.length() != null)
    if (${check.name()} == null) {
      problems.add("${check.name()}: required");
    } else if (${check.name()}.length() > ${check.length()}) {
      problems.add("${check.name()}: longer than ${check.length()} characters");
    }
    @elseif(check.required())
    if (${check.name()} == null) {
      problems.add("${check.name()}: required");
    }
    @else
    if (${check.name()} != null && ${check.name()}.length() > ${check.length()}) {
      problems.add("${check.name()}: longer than ${check.length()} characters");
    }
    @endif
    @endfor
    return problems;
    @endif
  }
}
