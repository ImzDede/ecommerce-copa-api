package br.ufc.smd.ecommercecopa.dto.address;

import java.util.List;

public record AddressListResponse(
        List<AddressResponse> items
) {
}
