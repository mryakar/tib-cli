@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
package ${entity.persistencePackage()};

@if(entity.uuidId())
import java.util.UUID;
@endif
import org.springframework.data.jpa.repository.JpaRepository;

public interface ${entity.repository()} extends JpaRepository<${entity.jpaEntity()}, ${entity.idType()}> {}
