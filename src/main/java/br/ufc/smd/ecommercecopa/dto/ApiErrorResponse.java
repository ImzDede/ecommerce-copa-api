package br.ufc.smd.ecommercecopa.dto;

import java.util.List;

public record ApiErrorResponse(ErrorBody error) {
    public record ErrorBody(String code, String message, List<FieldDetail> details) {
    }

    public record FieldDetail(String field, String message) {
    }
}
