package exception;

/** 404: unknown ID. */
public class NotFoundException extends ApiException {
    public NotFoundException(String what, int id) {
        super(404, "NOT_FOUND", what + " " + id + " not found");
    }
}
