@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.spring.JpaModel
@param EntityModel entity
@param JpaModel jpa
package ${entity.persistencePackage()};

@if(entity.uuidId())
import java.util.UUID;
@endif
import org.springframework.data.jpa.repository.JpaRepository;

public interface ${jpa.repository()} extends JpaRepository<${jpa.entityClass()}, ${entity.idType()}> {}
