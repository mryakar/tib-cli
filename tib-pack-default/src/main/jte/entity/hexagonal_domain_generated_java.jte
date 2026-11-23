@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.domainPackage()};

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
@if(entity.audited())
import java.time.Instant;
@endif

/** The values of {@link ${entity.name()}}, as the spec names them, in plain Java: the domain knows no framework. */
public abstract class ${entity.name()}Generated {

  private ${entity.idType()} ${entity.idField()};
  @for(var field : entity.fields())
  private ${field.javaType()} ${field.name()};
  @endfor
  @for(var relation : entity.relations())
  private ${relation.target().idType()} ${relation.idField()};
  @endfor
  @if(entity.audited())
  private Instant createdAt;
  private Instant updatedAt;
  @endif<%-- keeps the blank line below --%>

  public ${entity.idType()} getId() {
    return ${entity.idField()};
  }

  public void setId(${entity.idType()} ${entity.idField()}) {
    this.${entity.idField()} = ${entity.idField()};
  }
  @for(var field : entity.fields())<%-- keeps the blank line below --%>

  public ${field.javaType()} get${field.property()}() {
    return ${field.name()};
  }

  public void set${field.property()}(${field.javaType()} ${field.name()}) {
    this.${field.name()} = ${field.name()};
  }
  @endfor
  @for(var relation : entity.relations())<%-- keeps the blank line below --%>

  public ${relation.target().idType()} get${relation.idProperty()}() {
    return ${relation.idField()};
  }

  public void set${relation.idProperty()}(${relation.target().idType()} ${relation.idField()}) {
    this.${relation.idField()} = ${relation.idField()};
  }
  @endfor
  @if(entity.audited())<%-- keeps the blank line below --%>

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
  @endif
}
