@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.domainPackage()};

/** ${entity.article()} ${entity.variable()}, in plain Java. Its values are in {@link ${name}Generated}; the behavior goes here. */
public class ${name} extends ${name}Generated {}
