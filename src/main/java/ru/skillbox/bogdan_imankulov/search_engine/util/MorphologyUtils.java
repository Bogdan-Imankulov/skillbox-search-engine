package ru.skillbox.bogdan_imankulov.search_engine.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class MorphologyUtils {

	private final LuceneMorphology morphology;

	{
		try {
			morphology = new RussianLuceneMorphology();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public MorphologyUtils() {
	}

	public Map<String, Integer> getLemmasAndTheirFrequency(String textToAnalyze) {
		textToAnalyze = textToAnalyze.toLowerCase(Locale.ROOT);
		String[] words = Arrays.stream(textToAnalyze.split("\\s+"))
//                Delete "." "," and other non-alphabetic symbols, to turn "побежали!" into "побежали"
				.map(word -> word.replaceAll("[!:;'\",.?]+", ""))
				.filter(word -> word.matches("[ЁёА-я\\s]+"))
//                Get normal form of the word
				.map(morphology::getNormalForms)
//                Taking first el of a list, because list's size is always equal to 1
				.map(list -> list.get(0))
//                Regex matches only russian words, ".", "," and other symbols, except "-" are not matching,
//                because of words like "из-за"
				.toArray(String[]::new);
		return createLemmasAndFrequenciesFromText(words);
	}

	private Map<String, Integer> createLemmasAndFrequenciesFromText(String[] words) {
		Map<String, Integer> lemmasAndFrequency = new HashMap<>();

		for (String word : words) {
			String morphInfo = morphology.getMorphInfo(word).get(0);
			if (ifParticle(morphInfo)) {
				continue;
			}
			if (!lemmasAndFrequency.containsKey(word)) {
				lemmasAndFrequency.put(word, 1);
				continue;
			}
			lemmasAndFrequency.replace(word, lemmasAndFrequency.get(word) + 1);
		}
		return lemmasAndFrequency;
	}

	private boolean ifParticle(String morphInfo) {
		return morphInfo.contains("МЕЖД") ||
				morphInfo.contains("СОЮЗ") ||
				morphInfo.contains("ПРЕДЛ") ||
				morphInfo.contains("ЧАСТ");
	}

	public String lemmatizeSnippet(String snippet) {
		snippet = snippet.replaceAll("[^А-яёЁ0-9\\s]+", "");

		String[] splitSnippet = snippet.split("\\s+");
		StringBuilder lemmatizedSnippet = new StringBuilder();
		for (String str : splitSnippet) {
			if (str.isEmpty() || str.matches("[0-9]+")) {
				continue;
			}
			str = str.replaceAll("[0-9]+", "");
			str = str.toLowerCase().trim();
			str = morphology.getNormalForms(str).get(0);
			lemmatizedSnippet.append(" ").append(str);
		}
		return lemmatizedSnippet.toString();
	}
}
