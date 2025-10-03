package com.luckxpress.web.exception;

/**
 * Custom exception classes for the application
 */
public class CustomExceptions {

    /**
     * Exception for resource not found scenarios
     */
    public static class ResourceNotFoundException extends RuntimeException {
        private final String resourceName;
        private final String fieldName;
        private final Object fieldValue;

        public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
            super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
            this.resourceName = resourceName;
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
        }

        public String getResourceName() {
            return resourceName;
        }

        public String getFieldName() {
            return fieldName;
        }

        public Object getFieldValue() {
            return fieldValue;
        }
    }

    /**
     * Exception for bad request scenarios
     */
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }

        public BadRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception for unauthorized access
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    /**
     * Exception for forbidden access
     */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }

    /**
     * Exception for conflict scenarios
     */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    /**
     * Exception for business logic violations
     */
    public static class BusinessException extends RuntimeException {
        private final String errorCode;

        public BusinessException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    /**
     * Exception for external service failures
     */
    public static class ExternalServiceException extends RuntimeException {
        private final String serviceName;
        private final Integer statusCode;

        public ExternalServiceException(String serviceName, Integer statusCode, String message) {
            super(message);
            this.serviceName = serviceName;
            this.statusCode = statusCode;
        }

        public String getServiceName() {
            return serviceName;
        }

        public Integer getStatusCode() {
            return statusCode;
        }
    }
}
