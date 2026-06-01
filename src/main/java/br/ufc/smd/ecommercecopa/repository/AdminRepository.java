package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Admin;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
}
