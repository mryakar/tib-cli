@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
// Written by tib on every run. Do not edit: your changes would be lost.
package ${entity.packageName()};

${entity.repositoryImports()}public interface ${entity.name()}Repository extends JpaRepository<${entity.name()}, ${entity.idType()}> {}
