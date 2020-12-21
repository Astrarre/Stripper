package io.github.astrarre.stripper.gradle;

import java.io.ByteArrayInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;

import io.github.astrarre.stripper.Stripper;
import org.apache.tools.ant.util.ReaderInputStream;
import org.gradle.api.Action;
import org.gradle.api.file.FileCopyDetails;
import sun.nio.cs.StreamDecoder;

public class StripperAction implements Action<FileCopyDetails> {
	private static final Field ISR_SD;
	private static final Field SD_INP;

	static {
		try {
			ISR_SD = InputStreamReader.class.getDeclaredField("sd");
			ISR_SD.setAccessible(true);
			SD_INP = StreamDecoder.class.getDeclaredField("in");
			SD_INP.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	private static InputStream getInputStream(Reader reader) {
		String encoding = null;
		if (reader instanceof InputStreamReader) {
			try {
				return (InputStream) SD_INP.get(ISR_SD.get(reader));
			} catch (Throwable t) {
				encoding = ((InputStreamReader) reader).getEncoding();
			}
		}

		if (encoding != null) {
			return new ReaderInputStream(reader, encoding);
		} else {
			return new ReaderInputStream(reader);
		}
	}

	@Override
	public void execute(FileCopyDetails details) {
		String path = details.getPath();
		InputStream stream = details.open();
		try {
			if (path.endsWith(".class")) {
				byte[] bytes = Stripper.stripClas(path, stream);
				if(bytes == null) {
					details.exclude();
				} else {
					Map<String, Object> properties = new HashMap<>();
					properties.put("data", new InputStreamReader(new ByteArrayInputStream(bytes)));
					details.filter(properties, ByteReading.class);
				}
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static class ByteReading extends FilterReader {
		public Reader data;
		public ByteReading(Reader in) {
			super(in);
		}

		@Override
		public int read() throws IOException {
			this.lock = this.in = this.data;
			return super.read();
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			this.lock = this.in = this.data;
			return super.read(cbuf, off, len);
		}

		@Override
		public long skip(long n) throws IOException {
			this.lock = this.in = this.data;
			return super.skip(n);
		}

		@Override
		public boolean ready() throws IOException {
			this.lock = this.in = this.data;
			return super.ready();
		}

		@Override
		public boolean markSupported() {
			this.lock = this.in = this.data;
			return super.markSupported();
		}

		@Override
		public void mark(int readAheadLimit) throws IOException {
			this.lock = this.in = this.data;
			super.mark(readAheadLimit);
		}

		@Override
		public void reset() throws IOException {
			this.lock = this.in = this.data;
			super.reset();
		}

		@Override
		public void close() throws IOException {
			this.lock = this.in = this.data;
			super.close();
		}

		@Override
		public int read(CharBuffer target) throws IOException {
			this.lock = this.in = this.data;
			return super.read(target);
		}

		@Override
		public int read(char[] cbuf) throws IOException {
			this.lock = this.in = this.data;
			return super.read(cbuf);
		}
	}
}
