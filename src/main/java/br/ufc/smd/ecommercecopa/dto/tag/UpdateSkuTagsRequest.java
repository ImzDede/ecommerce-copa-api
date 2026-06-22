package br.ufc.smd.ecommercecopa.dto.tag;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record UpdateSkuTagsRequest(
        @NotNull(message = "Tags são obrigatórias")
        List<UUID> tagIds
) {
}
