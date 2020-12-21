package io.github.astrarre.stripper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import io.github.astrarre.stripper.impl.AccessStripper;
import io.github.astrarre.stripper.impl.CombinedProcessor;
import io.github.astrarre.stripper.impl.EnumStripper;
import io.github.astrarre.stripper.impl.HideAnnotationStripper;
import io.github.astrarre.stripper.impl.ImplementationStripper;
import io.github.astrarre.stripper.impl.OuterMethodStripper;
import io.github.astrarre.stripper.impl.StaticInitializerStripper;
import io.github.astrarre.stripper.util.ByteMapOutputStreamWriter;
import io.github.astrarre.stripper.util.ZipOutputStreamWriter;
import io.github.astrarre.stripper.util.ZipWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class Stripper {
	public static final CombinedProcessor PROCESSORS = new CombinedProcessor();

	static {
		PROCESSORS.add(new OuterMethodStripper());
		PROCESSORS.add(new AccessStripper());
		PROCESSORS.add(new StaticInitializerStripper());
		PROCESSORS.add(new HideAnnotationStripper());
		PROCESSORS.add(new ImplementationStripper());
		PROCESSORS.add(new EnumStripper());
	}

	public static void main(String[] args) throws IOException {
		strip(Collections.emptyList(), new File("fodder.jar"), new File("out_fodder.jar"));
	}

	public static void strip(List<String> filter, File input, File output) throws IOException {
		strip(filter, input, new ZipOutputStreamWriter(output));
	}

	/**
	 * @param filter a list of regexes to filter out files or folders, the pattern must match both .class and .java!
	 * @param inputFile the input jar
	 */
	public static void strip(List<String> filter, File inputFile, ZipWriter output) throws IOException {
		Predicate<String> patterns = filter.stream().map(Pattern::compile).map(Pattern::asPredicate).reduce(Predicate::and).orElse(s -> false);

		ZipFile file = new ZipFile(inputFile);
		Enumeration<? extends ZipEntry> enumeration = file.entries();
		byte[] buffer = new byte[4096];

		while (enumeration.hasMoreElements()) {
			ZipEntry entry = enumeration.nextElement();
			String name = entry.getName();

			if (patterns.test(name) || entry.isDirectory()) {
				continue;
			}

			InputStream input = file.getInputStream(entry);
			if (name.endsWith(".class")) {
				// asm strip
				byte[] data = stripClas(name, input);
				if (data != null) {
					output.openEntry(entry.getName());
					output.getOutput().write(data);
					output.closeEntry();
				}
			} else if (name.endsWith(".java")) {
				// source strip
				byte[] data = stripJava(name, new InputStreamReader(input));
				if (data != null) {
					output.openEntry(entry.getName());
					output.getOutput().write(data);
					output.closeEntry();
				}
			} else {
				output.openEntry(entry.getName());
				OutputStream out = output.getOutput();
				InputStream stream = file.getInputStream(entry);
				int i;
				while ((i = stream.read(buffer)) != -1) {
					out.write(buffer, 0, i);
				}
				output.closeEntry();
			}
		}

		file.close();
		output.close();
	}

	public static byte[] stripClas(String path, InputStream stream) throws IOException {
		ClassReader reader = new ClassReader(stream);
		ClassNode node = new ClassNode();
		reader.accept(node, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
		if (!PROCESSORS.apply(node)) {
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			node.accept(writer);
			return writer.toByteArray();
		}
		return null;
	}

	public static byte[] stripJava(String path, Reader reader) {
		CompilationUnit unit = StaticJavaParser.parse(reader);
		unit.setStorage(Paths.get(path));
		if (!PROCESSORS.apply(unit, unit.getPrimaryType().orElseThrow(() -> new IllegalStateException("Invalid java file: " + path)))) {
			return unit.toString().getBytes(StandardCharsets.UTF_8);
		}
		return null;
	}

	public static void strip(List<String> filter, File file) throws IOException {
		strip(filter, file, new ByteMapOutputStreamWriter(file));
	}
}
