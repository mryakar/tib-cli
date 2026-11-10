@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
!{String builder = name + "Builder";}
!{String dtoBuilder = name + "DtoBuilder";}
package ${entity.packageName()};

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
@for(var target : entity.otherTargets())
import ${target.packageName()}.${target.name()};
@endfor

/**
 * Sample ${entity.plural()} for the tests, with the first or the second sample values: a test
 * changes the value it is about through the builder.
 */
public final class ${name}Samples {

  private ${name}Samples() {}

  /** ${entity.article()} ${entity.variable()} with the first sample values@if(!entity.requiredRelations().isEmpty()), and the saved entities it needs@endif. */
  public static ${builder} ${entity.variable()}(${tests.requiredParameters()}) {
    return new ${builder}()${tests.sampleValues(1)};
  }

  /** ${entity.article()} ${entity.variable()} with the second sample values@if(!entity.requiredRelations().isEmpty()), and the saved entities it needs@endif. */
  public static ${builder} other${name}(${tests.requiredParameters()}) {
    return new ${builder}()${tests.sampleValues(2)};
  }
  @if(entity.dto())<%-- keeps the blank line below --%>

  /** ${entity.article()} ${entity.variable()} as the API takes it, with the first sample values@if(!entity.requiredRelations().isEmpty()), and the identifiers of the entities it needs@endif. */
  public static ${dtoBuilder} dto(${tests.requiredIdParameters()}) {
    return new ${dtoBuilder}()${tests.dtoSampleValues(1)};
  }

  /** ${entity.article()} ${entity.variable()} as the API takes it, with the second sample values@if(!entity.requiredRelations().isEmpty()), and the identifiers of the entities it needs@endif. */
  public static ${dtoBuilder} otherDto(${tests.requiredIdParameters()}) {
    return new ${dtoBuilder}()${tests.dtoSampleValues(2)};
  }

  /** ${entity.article()} ${entity.variable()} as the API takes it, without any value. */
  public static ${dtoBuilder} emptyDto() {
    return new ${dtoBuilder}();
  }
  @endif<%-- keeps the blank line below --%>

  /** ${entity.article()} ${entity.variable()} to build, one value at a time. */
  public static final class ${builder} {
    @template.entity.builder_members(builder = builder, properties = tests.builderProperties())

    public ${name} build() {
      ${name} ${entity.variable()} = new ${name}();
      @for(var field : entity.fields())
      ${entity.variable()}.set${field.property()}(${field.name()});
      @endfor
      @for(var relation : entity.relations())
      ${entity.variable()}.set${relation.property()}(${relation.field()});
      @endfor
      return ${entity.variable()};
    }
  }
  @if(entity.dto())<%-- keeps the blank line below --%>

  /** ${entity.article()} ${entity.variable()} as the API takes it, to build one value at a time@if(entity.audited()); its audit times are none@endif. */
  public static final class ${dtoBuilder} {
    @template.entity.builder_members(builder = dtoBuilder, properties = tests.dtoBuilderProperties())

    public ${name}Dto build() {
      return new ${name}Dto(${tests.dtoBuilderArguments()});
    }
  }
  @endif
}
