package ru.skillbox.bogdan_imankulov.search_engine;

import java.io.IOException;

public class TestExe {
	public static void main(String[] args) {
		try {
			String str = "http://timer.ru/online/sekundomer";
			System.out.println();
			throw new IOException(str.replaceFirst("http://timer.ru", ""));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
