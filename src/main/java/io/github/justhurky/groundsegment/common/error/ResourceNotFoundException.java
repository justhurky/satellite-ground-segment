package io.github.justhurky.groundsegment.common.error;

public class ResourceNotFoundException extends GroundSegmentException {

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resourceType + " not found: " + identifier);
    }
}
