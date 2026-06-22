package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Address;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByClient_UserIdAndDeletedAtIsNullOrderByDefaultAddressDescCityAscNeighborhoodAsc(UUID clientId);

    Optional<Address> findByIdAndClient_UserIdAndDeletedAtIsNull(UUID id, UUID clientId);
}
