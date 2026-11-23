@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
!{String variable = entity.variable();}
package ${entity.persistencePackage()};

import ${entity.domainPackage()}.${name};
import org.springframework.stereotype.Component;

/** Between the table and the domain: every value, the identifier@if(entity.audited()) and the audit times@endif included@if(!entity.relations().isEmpty()); a relation is the identifier of its target in both@endif. */
@Component
public class ${name}PersistenceMapper {

  public ${name} toDomain(${entity.jpaEntity()} entity) {
    ${name} ${variable} = new ${name}();
    ${variable}.setId(entity.getId());
    @for(var field : entity.fields())
    ${variable}.set${field.property()}(entity.get${field.property()}());
    @endfor
    @for(var relation : entity.relations())
    ${variable}.set${relation.idProperty()}(entity.get${relation.idProperty()}());
    @endfor
    @if(entity.audited())
    ${variable}.setCreatedAt(entity.getCreatedAt());
    ${variable}.setUpdatedAt(entity.getUpdatedAt());
    @endif
    return ${variable};
  }

  public ${entity.jpaEntity()} toEntity(${name} ${variable}) {
    ${entity.jpaEntity()} entity = new ${entity.jpaEntity()}();
    entity.setId(${variable}.getId());
    @for(var field : entity.fields())
    entity.set${field.property()}(${variable}.get${field.property()}());
    @endfor
    @for(var relation : entity.relations())
    entity.set${relation.idProperty()}(${variable}.get${relation.idProperty()}());
    @endfor
    @if(entity.audited())
    entity.setCreatedAt(${variable}.getCreatedAt());
    entity.setUpdatedAt(${variable}.getUpdatedAt());
    @endif
    return entity;
  }
}
