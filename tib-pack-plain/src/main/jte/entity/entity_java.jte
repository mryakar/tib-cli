@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.domainPackage()};

/** ${entity.article()} ${entity.variable()}. Its values are in {@link ${entity.name()}Generated}; the behavior goes here. */
public class ${entity.name()} extends ${entity.name()}Generated {}
