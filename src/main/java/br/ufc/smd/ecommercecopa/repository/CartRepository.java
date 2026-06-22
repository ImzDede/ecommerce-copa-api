package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Cart;
import br.ufc.smd.ecommercecopa.model.CartId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, CartId> {
    List<Cart> findByClient_UserIdOrderByCreatedAtAsc(UUID clientId);

    void deleteByClient_UserId(UUID clientId);
}
