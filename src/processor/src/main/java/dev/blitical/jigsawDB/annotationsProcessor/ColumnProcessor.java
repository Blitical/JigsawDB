package dev.blitical.jigsawDB.annotationsProcessor;

import com.google.auto.service.AutoService;
import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.annotations.PrimaryColumn;
import dev.blitical.jigsawDB.exceptions.compile.DuplicatePrimaryColumnException;
import dev.blitical.jigsawDB.exceptions.compile.MisusedAnnotationException;
import dev.blitical.jigsawDB.exceptions.compile.NoPrimaryColumnException;
import dev.blitical.jigsawDB.exceptions.compile.TableAndPrimaryColumnMismatch;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_25)
@SupportedAnnotationTypes("dev.blitical.jigsawDB.annotations.Column")
public final class ColumnProcessor extends AbstractProcessor {
    private Types types;
    private Filer filer;
    private Elements elements;
    private TypeElement tableElement;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        types = env.getTypeUtils();
        filer = env.getFiler();
        elements = env.getElementUtils();
        tableElement = elements.getTypeElement("dev.blitical.jigsawDB.table.Table");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeElement, List<VariableElement>> grouped = new HashMap<>();

        for (Element e : roundEnv.getElementsAnnotatedWith(Column.class)) {
            if (e.getKind() != ElementKind.FIELD) continue; // Just in case

            VariableElement field = (VariableElement) e;
            TypeElement owner = (TypeElement) field.getEnclosingElement();

            grouped.computeIfAbsent(owner, _ -> new ArrayList<>())
                    .add(field);
        }

        grouped.forEach(this::generate);
        return true;
    }

    private void generate(TypeElement entry, List<VariableElement> fields) {
        String entryName = entry.getSimpleName().toString();
        String className = entryName + "Fields";

        if (tableElement == null) {
            throw new IllegalStateException("Table type not found"); // Should never reach
        }

        TypeMirror tableType = tableElement.asType();
        TypeMirror entryType = entry.asType();

        if (!types.isAssignable(types.erasure(entryType), types.erasure(tableType))) {
            throw new MisusedAnnotationException(entryName, "@Column");
        }

        TypeMirror superclass = entry.getSuperclass();
        if (!(superclass instanceof DeclaredType declared)) {
            throw new MisusedAnnotationException(entryName, "@Column");
        }

        List<? extends TypeMirror> args = declared.getTypeArguments();
        if (args.size() != 2) {
            throw new MisusedAnnotationException(entryName, "@Column");
        }

        TypeMirror selfType = args.get(0);
        TypeMirror primaryKeyType = args.get(1);
        if (!types.isSameType(
                types.erasure(selfType),
                types.erasure(entryType)
        )) {
            throw new MisusedAnnotationException(entryName, "@Column");
        }

        String pkg = elements.getPackageOf(entry)
                .getQualifiedName()
                .toString();

        StringBuilder sb = new StringBuilder()
                .append("package ").append(pkg).append(";\n\n")
                .append("""
                        import javax.annotation.processing.Generated;
                        import dev.blitical.jigsawDB.entry.Field;
                        import dev.blitical.jigsawDB.entry.TypeToken;
                        import dev.blitical.jigsawDB.entry.fields.*;
                        import dev.blitical.jigsawDB.table.GeneratedTable;
                        
                        @Generated("DatabaseEntryProcessor")
                        public final class\s""")
                .append(className).append(" extends GeneratedTable<")
                .append(className).append(", ").append(entryName).append("> {\n\n");

        VariableElement previousPrimaryColumn = null;

        for (VariableElement f : fields) {
            String name = f.getSimpleName().toString();
            String type = makeNonPrimitive(
                    processingEnv.getTypeUtils().stripAnnotations(f.asType()).toString()
            );

            boolean isPrimaryColumn = false;
            PrimaryColumn primaryColumn = f.getAnnotation(PrimaryColumn.class);
            if (primaryColumn != null) {
                if (previousPrimaryColumn != null) {
                    throw new DuplicatePrimaryColumnException(
                            entryName,
                            f.getSimpleName().toString(),
                            previousPrimaryColumn.getSimpleName().toString()
                    );
                }

                if (!types.isSameType(
                        types.erasure(primaryKeyType),
                        types.erasure(f.asType())
                )) {
                    throw new TableAndPrimaryColumnMismatch(entryName, primaryKeyType.toString(), f.asType().toString());
                }

                previousPrimaryColumn = f;
                isPrimaryColumn = true;
            }

            String column = f.getAnnotation(Column.class).value();
            String fieldType = getFieldType(type, isPrimaryColumn);

            // public static final <TYPE>Field<CLASS, TYPE> VARIABLE = new Field<>("COLUMN");
            sb.append("    public static final ")
                    .append(fieldType).append("Field<")
                    .append(entryName).append(", ")
                    .append(type).append("> ")
                    .append(name)
                    .append(" = new ")
                    .append(fieldType).append("Field<>(\"")
                    .append(column).append("\", ")
                    .append("new TypeToken<").append(type).append(">() {}").append(");\n");
        }

        if (previousPrimaryColumn == null) {
            throw new NoPrimaryColumnException(entryName);
        }

        sb.append("\n    protected ").append(className).append("(")
                .append(entryName).append(" table) {")
                .append("\n        super(table);")
                .append("\n        this.self = this;")
                .append("\n    }");

        sb.append("\n}\n"); // Newline is for jyguy! so he doesn't lose his mind...

        write(pkg + "." + className, sb.toString(), entry);
    }

    private static String getFieldType(String type, boolean isPrimaryColumn) {
        String fieldType = switch (type) {
            case "java.lang.Integer" ->
                    isPrimaryColumn ? "Integer" : "Number"; // PrimaryIntegerField<>() OR NumberField<>()
            case "java.lang.Byte[]" -> isPrimaryColumn ? "Generic" : "Binary";
            case "java.lang.Long", "java.lang.Float", "java.lang.Double" ->
                    isPrimaryColumn ? "Generic" : "Number"; // PrimaryGenericField<>() OR NumberField<>()
            case null, default -> "Generic"; // GenericField<>()
        };
        if (isPrimaryColumn) {
            fieldType = "Primary" + fieldType;
        }
        return fieldType;
    }

    private void write(String fqcn, String code, Element origin) {
        try {
            JavaFileObject file = filer.createSourceFile(fqcn, origin);
            try (Writer w = file.openWriter()) {
                w.write(code);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String makeNonPrimitive(String t) {
        boolean isArray = t.endsWith("[]");
        if (isArray)
            t = t.substring(0, t.length() - 2);
        return switch (t) {
            case "int" -> "java.lang.Integer";
            case "long" -> "java.lang.Long";
            case "float" -> "java.lang.Float";
            case "double" -> "java.lang.Double";
            case "boolean" -> "java.lang.Boolean";
            case "byte" -> "java.lang.Byte";
            default -> t;
        } + (isArray ? "[]" : "");
    }
}
