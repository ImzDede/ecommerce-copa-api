package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    List<Tag> findAllByOrderByTextAsc();

    boolean existsByTextIgnoreCase(String text);

    boolean existsByTextIgnoreCaseAndIdNot(String text, UUID id);
}
