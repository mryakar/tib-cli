@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
// Written by tib on every run. Do not edit: your changes would be lost. Your code goes in ${entity.name()}.
package ${entity.packageName()};

${entity.generatedImports()}/** The columns of ${entity.table()}, as the spec names them; {@link ${entity.name()}} is the entity. */
@MappedSuperclass
public abstract class ${entity.name()}Generated {

${entity.declarations()}

${entity.accessors()}
}
