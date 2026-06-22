package br.ufc.smd.ecommercecopa.dto.tag;

import java.util.List;

public record TagListResponse(
        List<TagResponse> items
) {
}
