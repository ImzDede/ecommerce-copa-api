package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.SkuTag;
import br.ufc.smd.ecommercecopa.model.SkuTagId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuTagRepository extends JpaRepository<SkuTag, SkuTagId> {
    List<SkuTag> findBySku_Id(UUID skuId);

    boolean existsByTag_Id(UUID tagId);

    void deleteBySku_Id(UUID skuId);
}
