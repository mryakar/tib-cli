@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@import me.yakar.tib.pack.Sources
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
!{String builder = name + "Builder";}
!{String dtoBuilder = name + "DtoBuilder";}
package ${entity.packageName()};

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
${Sources.importOf(entity.packageName(), entity.domainPackage(), name)}
@if(entity.dto())
${Sources.importOf(entity.packageName(), entity.webPackage(), name + "Dto")}
@endif
@if(!entity.refersById())
@for(var target : entity.otherTargets())
import ${target.packageName()}.${target.name()};
@endfor
@endif

/**
 * Sample ${entity.plural()} for the tests, with the first or the second sample values: a test
 * changes the value it is about through the builder.
 */
public final class ${name}Samples {

  private ${name}Samples() {}

  /** ${entity.article()} ${entity.variable()} with the first sample values@if(!entity.requiredRelations().isEmpty() && entity.refersById()), and the identifiers of the saved entities it needs@elseif(!entity.requiredRelations().isEmpty()), and the saved entities it needs@endif. */
  public static ${builder} ${entity.variable()}(${tests.requiredParameters()}) {
    return new ${builder}()${tests.sampleValues(1)};
  }

  /** ${entity.article()} ${entity.variable()} with the second sample values@if(!entity.requiredRelations().isEmpty() && entity.refersById()), and the identifiers of the saved entities it needs@elseif(!entity.requiredRelations().isEmpty()), and the saved entities it needs@endif. */
  public static ${builder} other${name}(${tests.requiredParameters()}) {
    return new ${builder}()${tests.sampleValues(2)};
  }
  @if(entity.dto())

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
  @endif

  /** ${entity.article()} ${entity.variable()} to build, one value at a time. */
  public static final class ${builder} {
    @template.entity.builder_members(builder = builder, properties = tests.builderProperties())

    public ${name} build() {
      ${name} ${entity.variable()} = new ${name}();
      @for(var field : entity.fields())
      ${entity.variable()}.set${field.property()}(this.${field.name()});
      @endfor
      @for(var relation : entity.relations())
      @if(entity.refersById())
      ${entity.variable()}.set${relation.idProperty()}(this.${relation.idField()});
      @else
      ${entity.variable()}.set${relation.property()}(this.${relation.field()});
      @endif
      @endfor
      return ${entity.variable()};
    }
  }
  @if(entity.dto())

  /** ${entity.article()} ${entity.variable()} as the API takes it, to build one value at a time@if(entity.audited()); its audit times are none@endif. */
  public static final class ${dtoBuilder} {
    @template.entity.builder_members(builder = dtoBuilder, properties = tests.dtoBuilderProperties())

    public ${name}Dto build() {
      return new ${name}Dto(${tests.dtoBuilderArguments()});
    }
  }
  @endif
}
