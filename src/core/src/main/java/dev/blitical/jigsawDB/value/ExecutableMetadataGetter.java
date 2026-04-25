package dev.blitical.jigsawDB.value;

public class ExecutableMetadataGetter {

    public record Result(
            Long start,
            Long end,
            Long duration,
            boolean timedOut
    ) {
    }

    protected void setResult(Result result) {
    }
}
