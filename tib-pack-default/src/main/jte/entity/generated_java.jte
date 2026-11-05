@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.spec.Entity
@param EntityModel entity
package ${entity.packageName()};

${entity.generatedImports()}/** The columns of ${entity.table()}, as the spec names them; {@link ${entity.name()}} is the entity. */
@MappedSuperclass
public abstract class ${entity.name()}Generated {

  @for(var annotation : entity.idAnnotations())
  ${annotation}
  @endfor
  @Column(name = "${entity.idColumn()}", nullable = false)
  private ${entity.idType()} ${entity.idField()};

  @for(var field : entity.fields())
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
  @Column(name = "${Entity.CREATED_AT}", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "${Entity.UPDATED_AT}", nullable = false)
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
