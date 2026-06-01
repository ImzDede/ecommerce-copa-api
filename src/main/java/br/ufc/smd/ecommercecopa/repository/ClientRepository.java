package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Client;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    boolean existsByCpf(String cpf);
}
