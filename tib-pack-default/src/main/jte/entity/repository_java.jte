@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
// Written by tib when the entity was created; it is yours from then on.
package ${entity.packageName()};

${entity.repositoryImports()}public interface ${entity.name()}Repository extends JpaRepository<${entity.name()}, ${entity.idType()}> {}
