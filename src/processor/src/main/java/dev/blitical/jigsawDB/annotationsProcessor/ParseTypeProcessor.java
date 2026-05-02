package dev.blitical.jigsawDB.annotationsProcessor;

import com.google.auto.service.AutoService;
import dev.blitical.jigsawDB.annotations.Parse;
import dev.blitical.jigsawDB.encoder.Encoder.CheckContext;
import dev.blitical.jigsawDB.encoder.Encoder.CheckResult;
import dev.blitical.jigsawDB.encoder.ParseType;
import dev.blitical.jigsawDB.encoder.encoderTypes.*;
import dev.blitical.jigsawDB.exceptions.encoder.IllegalParseTypeException;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_25)
@SupportedAnnotationTypes("dev.blitical.jigsawDB.annotations.Parse")
public final class ParseTypeProcessor extends AbstractProcessor {
    private ProcessingEnvironment env;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.env = env;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(Parse.class)) {
            checkParseType(e);
        }
        return true;
    }

    private void checkParseType(Element e) {
        Parse annotation = e.getAnnotation(Parse.class);
        if (annotation == null) return;

        Set<ElementKind> allowed = Set.of(
                ElementKind.FIELD,
                ElementKind.TYPE_PARAMETER,
                ElementKind.CLASS,
                ElementKind.INTERFACE,
                ElementKind.ENUM
        );
        if (!allowed.contains(e.getKind()))
            return;

        TypeMirror type = e.asType();
        CheckContext context = new CheckContext(
                type,
                e,
                env
        );

        CheckResult result = switch (annotation.value()) {
            case STRING -> checkClasses(context, ParseType.PREDEFINED_TYPES.get(ParseType.STRING));
            case INTEGER -> checkClasses(context, ParseType.PREDEFINED_TYPES.get(ParseType.INTEGER));
            case REAL -> checkClasses(context, ParseType.PREDEFINED_TYPES.get(ParseType.REAL));
            case BLOB -> checkClasses(context, ParseType.PREDEFINED_TYPES.get(ParseType.BLOB));
            case JSON -> JsonEncoder.check(context);
            case JAVA_SERIALIZED -> JavaSerializedEncoder.check(context);
            case ENUM_STRING -> EnumStringEncoder.check(context);
            case ENUM_ORDINAL -> EnumOrdinalEncoder.check(context);
            case UUID_STRING -> UUIDStringEncoder.check(context);
            case TEMPORAL_EPOCH -> TemporalEpochEncoder.check(context);
            case TEMPORAL_ISO -> TemporalIsoEncoder.check(context);
            case BINARY -> BinaryEncoder.check(context);
        };

        if (result != null && !result.passed()) {
            throw new IllegalParseTypeException(formatTypeError(e, result.customError()));
        }
    }

    private CheckResult checkClasses(CheckContext context, Class<?>... classes) {
        Set<String> names = new HashSet<>();
        for (var clazz : classes) {
            names.add(clazz.getName());
            if (clazz.equals(context.type().getClass())) {
                return new CheckResult(true, null);
            }
        }
        return new CheckResult(false,
                "Can only be parsed into: "
                        + String.join(", ", names.toArray(new String[0]))
        );
    }

    private String formatTypeError(Element e, String customError) {
        StringBuilder error = new StringBuilder();
        String fallback = "Type";
        Parse annotation = e.getAnnotation(Parse.class);
        if (annotation == null) return fallback;

        switch (e.getKind()) {
            case FIELD -> {
                VariableElement ve = (VariableElement) e;
                error.append("Field '")
                        .append(ve.getSimpleName().toString())
                        .append("' ")
                        .append("(type '")
                        .append(ve.asType())
                        .append("') ");
            }
            case TYPE_PARAMETER -> {
                TypeParameterElement tpe = (TypeParameterElement) e;
                error.append("Type parameter '").append(tpe.getSimpleName()).append("' ");
            }
            case CLASS, INTERFACE, ENUM -> {
                TypeElement te = (TypeElement) e;
                error.append("Class '").append(te.getSimpleName()).append("' ");
            }
            default -> error.append(fallback).append(" '").append(e.getSimpleName()).append("' ");
        }
        error.append("cannot have a parseType of ").append(annotation.value());

        if (customError != null) {
            error.append(" - ").append(customError);
        }

        return error.toString();
    }
}
