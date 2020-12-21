package io.github.stripper.test;

import io.github.astrarre.stripper.Hide;

public class Test {
	public static void main(String[] args) {
		System.out.println("Hello!");
	}

	private void privateMethod() {
		System.out.println("wot!");
	}

	public void publicMethod() {
		System.out.println("Implementation...");
	}

	@Hide
	protected void hiddenMethod() {
		System.out.println("Implementation!");
	}
}
