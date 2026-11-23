@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.domainPackage()};

@if(entity.uuidId())
import java.util.UUID;
@endif
import org.springframework.data.jpa.repository.JpaRepository;

public interface ${entity.name()}Repository extends JpaRepository<${entity.name()}, ${entity.idType()}> {}
