@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.domainPackage()};

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** ${entity.article()} ${entity.variable()}. The columns are in {@link ${name}Generated}; the behavior goes here. */
@Entity
@Table(name = "${entity.table()}")
public class ${name} extends ${name}Generated {}
