@import java.util.List
@import me.yakar.tib.pack.EntityTestsModel.Property
@param String builder
@param List<Property> properties
    @for(var property : properties)
    private ${property.type()} ${property.name()};
    @endfor

    private ${builder}() {}
    @for(var property : properties)

    public ${builder} ${property.name()}(${property.type()} ${property.name()}) {
      this.${property.name()} = ${property.name()};
      return this;
    }
    @endfor
