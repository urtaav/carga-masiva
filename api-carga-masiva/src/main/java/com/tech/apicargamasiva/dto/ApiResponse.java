package com.tech.apicargamasiva.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int status;
    private String statusMessage;
    private boolean success;
    private T data;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public ApiResponse(HttpStatus httpStatus, boolean success, T data, String message) {
        this.status = httpStatus.value();
        this.statusMessage = httpStatus.getReasonPhrase();
        this.success = success;
        this.data = data;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(HttpStatus.OK, true, data, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.OK, true, data, "Operación exitosa");
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(HttpStatus.OK, true, null, message);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return new ApiResponse<>(status, false, null, message);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, T data, String message) {
        return new ApiResponse<>(status, false, data, message);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(HttpStatus.NOT_FOUND, false, null, message);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(HttpStatus.BAD_REQUEST, false, null, message);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(HttpStatus.CREATED, true, data, message);
    }
}
