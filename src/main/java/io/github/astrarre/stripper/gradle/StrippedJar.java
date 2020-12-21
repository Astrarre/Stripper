package io.github.astrarre.stripper.gradle;

import org.gradle.api.tasks.bundling.Jar;

public class StrippedJar extends Jar {
	public StrippedJar() {
		this.getMainSpec().appendCachingSafeCopyAction(new StripperAction());
	}
}
