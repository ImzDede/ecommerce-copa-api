package br.ufc.smd.ecommercecopa.dto.review;

import java.util.List;

public record ReviewListResponse(
        List<ReviewResponse> items
) {
}
