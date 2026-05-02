package dev.blitical.jigsawDB.exceptions.compile;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException;

@JigsawDBException(
        severity = JigsawDBException.Severity.SEVERE,
        fixes = {
                "Removing the annotation",
                "Ensuring that your class extends the correct superclass (eg: YourTable extends Table<YourTable, PrimaryKey>)",
                "Ensuring that all values annotated with @PrimaryColumn are also annotated with @Column(\"...\")"
        }
)
public class MisusedAnnotationException extends CompileException {
    public MisusedAnnotationException(String tableName, String annotationName) {
        super(String.format("Misused annotation in '%s': '%s'", tableName, annotationName));
    }
}
