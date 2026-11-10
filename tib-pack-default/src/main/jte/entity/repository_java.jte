@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.packageName()};

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ${entity.name()}Repository extends JpaRepository<${entity.name()}, ${entity.idType()}> {}
