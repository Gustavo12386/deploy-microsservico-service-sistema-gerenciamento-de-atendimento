package com.service.domain.repository;
import com.service.domain.model.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID>  {

}
