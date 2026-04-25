package dev.blitical.jigsawDB.annotationsProcessor;

import com.google.auto.service.AutoService;
import dev.blitical.jigsawDB.annotations.Column;
import dev.blitical.jigsawDB.annotations.PrimaryColumn;
import dev.blitical.jigsawDB.exceptions.compile.MisusedAnnotationException;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_25)
@SupportedAnnotationTypes("dev.blitical.jigsawDB.annotations.PrimaryColumn")
public final class PrimaryColumnProcessor extends AbstractProcessor {
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(PrimaryColumn.class)) {
            if (e.getAnnotation(Column.class) == null) {
                TypeElement owner = (TypeElement) e.getEnclosingElement();
                throw new MisusedAnnotationException(
                        owner.getSimpleName().toString(),
                        "@PrimaryColumn"
                );
            }
        }
        return true;
    }
}
