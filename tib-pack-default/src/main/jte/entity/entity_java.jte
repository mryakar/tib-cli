@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.packageName()};

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** ${entity.article()} ${entity.variable()}. The columns are in {@link ${entity.name()}Generated}; the behavior goes here. */
@Entity
@Table(name = "${entity.table()}")
public class ${entity.name()} extends ${entity.name()}Generated {}
