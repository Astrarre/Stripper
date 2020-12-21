package io.github.astrarre.stripper.impl;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import io.github.astrarre.stripper.AbstractProcessor;
import io.github.astrarre.stripper.Hide;
import io.github.astrarre.stripper.asm.AsmUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class HideAnnotationStripper implements AbstractProcessor {
	@Override
	public boolean apply(CompilationUnit unit, TypeDeclaration<?> primary) {
		if(primary.isAnnotationPresent(Hide.class)) {
			return true;
		}
		for (MarkerAnnotationExpr expr : unit.findAll(MarkerAnnotationExpr.class)) {
			if (expr.getNameAsString().equals(Hide.class.getSimpleName())) {
				expr.getParentNode().ifPresent(Node::remove);
			}
		}
		return false;
	}

	@Override
	public boolean apply(ClassNode node) {
		if(hasAnnotation(node.invisibleAnnotations, Hide.class)) {
			return true;
		}

		Iterator<MethodNode> iterator = node.methods.iterator();
		while (iterator.hasNext()) {
			MethodNode method = iterator.next();
			if (hasAnnotation(method.invisibleAnnotations, Hide.class)) {
				iterator.remove();
			} else {
				method.instructions.clear();
				AsmUtil.visitStub(method);
			}
		}

		node.fields.removeIf(field -> hasAnnotation(field.invisibleAnnotations, Hide.class));
		return false;
	}

	public static boolean hasAnnotation(List<AnnotationNode> nodes, Class<? extends Annotation> anno) {
		String desc = Type.getDescriptor(anno);
		if (nodes == null) {
			return false;
		}
		for (AnnotationNode node : nodes) {
			if (desc.equals(node.desc)) {
				return true;
			}
		}
		return false;
	}
}
