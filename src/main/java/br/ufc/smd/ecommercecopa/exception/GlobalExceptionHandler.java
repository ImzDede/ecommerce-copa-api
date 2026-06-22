package br.ufc.smd.ecommercecopa.exception;

import br.ufc.smd.ecommercecopa.dto.ApiErrorResponse;
import br.ufc.smd.ecommercecopa.dto.ApiErrorResponse.ErrorBody;
import br.ufc.smd.ecommercecopa.dto.ApiErrorResponse.FieldDetail;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex) {
        LOGGER.warn("AppException: code={}, message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiErrorResponse(new ErrorBody(ex.getCode(), ex.getMessage(), List.of())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        LOGGER.warn("Validation error: {}", ex.getMessage());
        List<FieldDetail> details = mapFieldErrors(ex.getBindingResult().getFieldErrors());

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(new ErrorBody("VALIDATION_ERROR", "Dados inválidos", details)));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException ex) {
        LOGGER.warn("Bind error: {}", ex.getMessage());
        List<FieldDetail> details = mapFieldErrors(ex.getBindingResult().getFieldErrors());

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(new ErrorBody("VALIDATION_ERROR", "Dados inválidos", details)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        LOGGER.warn("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(new ErrorBody("VALIDATION_ERROR", "Dados inválidos", List.of())));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        LOGGER.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(new ErrorBody("DUPLICATE_RESOURCE", "Violação de unicidade ou integridade", List.of())));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        LOGGER.warn("Unsupported media type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ApiErrorResponse(new ErrorBody(
                        "UNSUPPORTED_MEDIA_TYPE",
                        "Content-Type deve ser application/json ou multipart/form-data, conforme a rota",
                        List.of()
                )));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestPart(MissingServletRequestPartException ex) {
        LOGGER.warn("Missing multipart part: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(new ErrorBody(
                        "VALIDATION_ERROR",
                        "Dados inválidos",
                        List.of(new FieldDetail(ex.getRequestPartName(), "Campo multipart obrigatório"))
                )));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        LOGGER.warn("Max upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiErrorResponse(new ErrorBody("UPLOAD_TOO_LARGE", "Arquivo excede o tamanho máximo permitido", List.of())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        LOGGER.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(new ErrorBody("INTERNAL_ERROR", "Erro interno", List.of())));
    }

    private FieldDetail mapFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() == null ? "Valor inválido" : fieldError.getDefaultMessage();
        return new FieldDetail(fieldError.getField(), message);
    }

    private List<FieldDetail> mapFieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(this::mapFieldError)
                .toList();
    }
}
