package io.deskpilot.engine.recording;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {
    public enum Status { OK, WARN, REJECT }

    private Status status;
    private final List<String> messages = new ArrayList<>();

    private ValidationResult(Status status) { this.status = status; }

    public static ValidationResult ok() { return new ValidationResult(Status.OK); }

    public static ValidationResult warn(String msg) {
        ValidationResult r = new ValidationResult(Status.WARN);
        r.messages.add(msg);
        return r;
    }

    public static ValidationResult reject(String msg) {
        ValidationResult r = new ValidationResult(Status.REJECT);
        r.messages.add(msg);
        return r;
    }

    public ValidationResult addWarn(String msg) {
        if (status == Status.OK) status = Status.WARN;
        messages.add(msg);
        return this;
    }

    public ValidationResult addReject(String msg) {
        status = Status.REJECT;
        messages.add(msg);
        return this;
    }

    public Status status() { return status; }
    public List<String> messages() { return Collections.unmodifiableList(messages); }

    public boolean isOkOrWarn() { return status != Status.REJECT; }

    @Override
    public String toString() {
        return status + (messages.isEmpty() ? "" : " - " + String.join(" | ", messages));
    }
}
