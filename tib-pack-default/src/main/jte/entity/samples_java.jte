@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
!{String builder = name + "Builder";}
!{String dtoBuilder = name + "DtoBuilder";}
package ${entity.packageName()};

${tests.samplesImports()}/**
 * Sample ${entity.plural()} for the tests, with the first or the second sample values: a test
 * changes the value it is about through the builder.
 */
public final class ${name}Samples {

  private ${name}Samples() {}

  /** ${tests.article()} ${entity.variable()} with the first sample values@if(!entity.requiredRelations().isEmpty()), and the saved entities it needs@endif. */
  public static ${builder} ${entity.variable()}(${tests.requiredParameters()}) {
    return new ${builder}()@for(var field : entity.fields()).${field.name()}(${field.sample(1)})@endfor@for(var relation : entity.requiredRelations()).${relation.field()}(${relation.field()})@endfor;
  }

  /** ${tests.article()} ${entity.variable()} with the second sample values@if(!entity.requiredRelations().isEmpty()), and the saved entities it needs@endif. */
  public static ${builder} other${name}(${tests.requiredParameters()}) {
    return new ${builder}()@for(var field : entity.fields()).${field.name()}(${field.sample(2)})@endfor@for(var relation : entity.requiredRelations()).${relation.field()}(${relation.field()})@endfor;
  }
  @if(tests.dto())<%-- keeps the blank line below --%>

  /** ${tests.article()} ${entity.variable()} as the API takes it, with the first sample values@if(!entity.requiredRelations().isEmpty()), and the identifiers of the entities it needs@endif. */
  public static ${dtoBuilder} dto(${tests.requiredIdParameters()}) {
    return new ${dtoBuilder}()@for(var field : entity.fields()).${field.name()}(${field.sample(1)})@endfor@for(var relation : entity.requiredRelations()).${relation.idField()}(${relation.idField()})@endfor;
  }

  /** ${tests.article()} ${entity.variable()} as the API takes it, with the second sample values@if(!entity.requiredRelations().isEmpty()), and the identifiers of the entities it needs@endif. */
  public static ${dtoBuilder} otherDto(${tests.requiredIdParameters()}) {
    return new ${dtoBuilder}()@for(var field : entity.fields()).${field.name()}(${field.sample(2)})@endfor@for(var relation : entity.requiredRelations()).${relation.idField()}(${relation.idField()})@endfor;
  }

  /** ${tests.article()} ${entity.variable()} as the API takes it, without any value. */
  public static ${dtoBuilder} emptyDto() {
    return new ${dtoBuilder}();
  }
  @endif<%-- keeps the blank line below --%>

  /** ${tests.article()} ${entity.variable()} to build, one value at a time. */
  public static final class ${builder} {
    @for(var field : entity.fields())
    private ${field.javaType()} ${field.name()};
    @endfor
    @for(var relation : entity.relations())
    private ${relation.target().name()} ${relation.field()};
    @endfor

    private ${builder}() {}
    @for(var field : entity.fields())

    public ${builder} ${field.name()}(${field.javaType()} ${field.name()}) {
      this.${field.name()} = ${field.name()};
      return this;
    }
    @endfor
    @for(var relation : entity.relations())

    public ${builder} ${relation.field()}(${relation.target().name()} ${relation.field()}) {
      this.${relation.field()} = ${relation.field()};
      return this;
    }
    @endfor

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
  @if(tests.dto())<%-- keeps the blank line below --%>

  /** ${tests.article()} ${entity.variable()} as the API takes it, to build one value at a time; its audit times are none. */
  public static final class ${dtoBuilder} {
    private ${entity.idType()} ${entity.idField()};
    @for(var field : entity.fields())
    private ${field.javaType()} ${field.name()};
    @endfor
    @for(var relation : entity.relations())
    private ${relation.target().idType()} ${relation.idField()};
    @endfor

    private ${dtoBuilder}() {}

    public ${dtoBuilder} ${entity.idField()}(${entity.idType()} ${entity.idField()}) {
      this.${entity.idField()} = ${entity.idField()};
      return this;
    }
    @for(var field : entity.fields())

    public ${dtoBuilder} ${field.name()}(${field.javaType()} ${field.name()}) {
      this.${field.name()} = ${field.name()};
      return this;
    }
    @endfor
    @for(var relation : entity.relations())

    public ${dtoBuilder} ${relation.idField()}(${relation.target().idType()} ${relation.idField()}) {
      this.${relation.idField()} = ${relation.idField()};
      return this;
    }
    @endfor

    public ${name}Dto build() {
      return new ${name}Dto(${tests.dtoBuilderArguments()});
    }
  }
  @endif
}
