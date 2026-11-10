@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.packageName()};

@if(entity.uuid())
import java.util.UUID;
@endif
import org.springframework.data.jpa.repository.JpaRepository;

public interface ${entity.name()}Repository extends JpaRepository<${entity.name()}, ${entity.idType()}> {}
