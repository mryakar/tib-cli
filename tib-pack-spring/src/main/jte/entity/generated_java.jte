@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.spring.JpaModel
@param EntityModel entity
@param JpaModel jpa
package ${entity.persistencePackage()};

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
@if(entity.hexagonal())
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
@else
import jakarta.persistence.MappedSuperclass;
@endif
@if(entity.idSequence())
import jakarta.persistence.SequenceGenerator;
@endif
@for(var relation : entity.relations())
@if(entity.refersById())
@if(relation.target().uuidId())
import java.util.UUID;
@endif
@else
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.${jpa.annotation(relation)};
@endif
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
@if(!entity.refersById())
@for(var target : entity.otherTargets())
import ${target.packageName()}.${target.name()};
@endfor
@endif<%-- keeps the blank line below --%>

@if(entity.hexagonal())
/**
 * The table ${entity.table()}, its columns as the spec names them; {@link ${entity.name()}PersistenceMapper} maps it to and
 * from the domain.
 */
@Entity
@Table(name = "${entity.table()}")
public class ${jpa.entityClass()} {
@else
/** The columns of ${entity.table()}, as the spec names them; {@link ${entity.name()}} is the entity. */
@MappedSuperclass
public abstract class ${entity.name()}Generated {
@endif<%-- keeps the blank line below --%>

  @for(var annotation : jpa.idAnnotations())
  ${annotation}
  @endfor
  @Column(name = "${entity.idColumn()}")
  private ${entity.idType()} ${entity.idField()};

  @for(var field : entity.fields())
  @if(field.text())
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @endif
  ${jpa.column(field)}
  private ${field.javaType()} ${field.name()};

  @endfor
  @for(var relation : entity.relations())
  @if(entity.refersById())
  ${jpa.column(relation)}
  private ${relation.target().idType()} ${relation.idField()};
  @else
  ${jpa.mapping(relation)}
  ${jpa.joinColumn(relation)}
  private ${relation.target().name()} ${relation.field()};
  @endif

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

  @if(entity.refersById())
  public ${relation.target().idType()} get${relation.idProperty()}() {
    return ${relation.idField()};
  }

  public void set${relation.idProperty()}(${relation.target().idType()} ${relation.idField()}) {
    this.${relation.idField()} = ${relation.idField()};
  }
  @else
  public ${relation.target().name()} get${relation.property()}() {
    return ${relation.field()};
  }

  public void set${relation.property()}(${relation.target().name()} ${relation.field()}) {
    this.${relation.field()} = ${relation.field()};
  }
  @endif
  @endfor
  @if(entity.audited())<%-- keeps the blank line below --%>

  public Instant getCreatedAt() {
    return createdAt;
  }
  @if(entity.hexagonal())<%-- keeps the blank line below --%>

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
  @endif<%-- keeps the blank line below --%>

  public Instant getUpdatedAt() {
    return updatedAt;
  }
  @if(entity.hexagonal())<%-- keeps the blank line below --%>

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
  @endif
  @endif
}
