@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.packageName()};

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
@if(entity.idSequence())
import jakarta.persistence.SequenceGenerator;
@endif
@for(var relation : entity.relations())
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.${relation.annotation()};
@endfor
@if(entity.uuidId())
import java.util.UUID;
@endif
@for(var field : entity.fields())
${field.typeImport()}
@if(field.text())
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@endif
@endfor
@if(entity.audited())
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
@endif
@for(var target : entity.otherTargets())
import ${target.packageName()}.${target.name()};
@endfor

/** The columns of ${entity.table()}, as the spec names them; {@link ${entity.name()}} is the entity. */
@MappedSuperclass
public abstract class ${entity.name()}Generated {

  @for(var annotation : entity.idAnnotations())
  ${annotation}
  @endfor
  @Column(name = "${entity.idColumn()}")
  private ${entity.idType()} ${entity.idField()};

  @for(var field : entity.fields())
  @if(field.text())
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @endif
  ${field.columnAnnotation()}
  private ${field.javaType()} ${field.name()};

  @endfor
  @for(var relation : entity.relations())
  ${relation.mapping()}
  ${relation.joinColumn()}
  private ${relation.target().name()} ${relation.field()};

  @endfor
  @if(entity.audited())
  @CreationTimestamp
  @Column(name = "${entity.createdAt()}", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "${entity.updatedAt()}", nullable = false)
  private Instant updatedAt;

  @endif
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

  public ${relation.target().name()} get${relation.property()}() {
    return ${relation.field()};
  }

  public void set${relation.property()}(${relation.target().name()} ${relation.field()}) {
    this.${relation.field()} = ${relation.field()};
  }
  @endfor
  @if(entity.audited())<%-- keeps the blank line below --%>

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
  @endif
}
