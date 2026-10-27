@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
// Written by tib on every run. Do not edit: your changes would be lost.
package ${entity.packageName()};

${entity.dtoImports()}/** A ${entity.variable()} as the API shows and takes it; a relation is the identifier of its target. */
public record ${entity.name()}Dto(
    ${entity.dtoComponents()}) {}
