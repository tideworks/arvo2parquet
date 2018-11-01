/* ByteCodePatchingAnnotationProcessor.java
 *
 * Copyright July 2018 Tideworks Technology
 * Author: Roger D. Voss
 * MIT License
 */
package com.tideworks.annotation;

import com.tideworks.data_load.ValidateAvroSchema;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.AnnotationFormatError;
import java.util.Set;

@SupportedAnnotationTypes("com.tideworks.annotation.InvokeByteCodePatching")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ByteCodePatchingAnnotationProcessor extends AbstractProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) return true;

    // looking for at least one class annotated with @InvokeByteCodePatching
    final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(InvokeByteCodePatching.class);
    if (elements.size() <= 0) return false;

    //noinspection LoopStatementThatDoesntLoop
    for (final Element elem : elements) {
      // is invalid to apply this annotation to anything other than a class type
      if (!elem.getKind().isClass() || !(elem instanceof QualifiedNameable)) {
        throw new AnnotationFormatError(elem.toString() + " Java type not supported by " + InvokeByteCodePatching.class);
      }

      InvokeByteCodePatching annotation = elem.getAnnotation(InvokeByteCodePatching.class);
      if (annotation == null) {
        throw new AnnotationFormatError("invalid annotation " + InvokeByteCodePatching.class);
      }

      break; // found a class marked by the @InvokeByteCodePatching annotation
    }

    try {
      final String classesBldDir = System.getProperty("maven.build.classes.dir", "target/classes");
      ValidateAvroSchema.bytecodePatchAvroSchemaClass(new File(classesBldDir));
    } catch (ClassNotFoundException|IOException e) {
      uncheckedExceptionThrow(e);
    }

    return true; // no further processing of this annotation type
  }

  @SuppressWarnings({"unchecked", "UnusedReturnValue"})
  private static <T extends Throwable, R> R uncheckedExceptionThrow(Throwable t) throws T { throw (T) t; }
}