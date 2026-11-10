@import gg.jte.support.ForSupport
@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.packageName()};

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ${entity.article()} ${entity.variable()} as the API shows and takes it@if(!entity.relations().isEmpty()); a relation is the identifier of its target@endif.
 @if(entity.audited())
 * The identifier and the audit times are read only; the service ignores them.
 @else
 * The identifier is read only; the service ignores it.
 @endif
 */
public record ${entity.name()}Dto(
    @for(var component : ForSupport.of(entity.dtoComponents()))
    ${component.get()}${component.isLast() ? ") {}" : ","}
    @endfor
