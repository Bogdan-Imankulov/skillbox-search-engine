package ru.skillbox.bogdan_imankulov.search_engine.services.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import ru.skillbox.bogdan_imankulov.search_engine.model.Lemma;
import ru.skillbox.bogdan_imankulov.search_engine.services.SearchDataCreatingService;
import ru.skillbox.bogdan_imankulov.search_engine.util.MorphologyUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchDataCreatingServiceImpl implements SearchDataCreatingService {


	private static final MorphologyUtils morphologyUtils = new MorphologyUtils();

	public static String formatSnippet(String content, List<Lemma> searchLemmas) {
		String formattedContent = content.replaceAll("<body[A-zА-я0-9-,.'\"()\\s=]*>", "<body>");
		formattedContent = formattedContent.substring(formattedContent.indexOf("<body>") + 6, formattedContent.indexOf("</body>")).trim();
		Element htmlElementFromSnippet = Jsoup.parse(formattedContent);
		formattedContent = htmlElementFromSnippet.wholeText().replaceAll("\\s+", " ");
		formattedContent = substringAroundLemma(formattedContent, searchLemmas);
		List<String> lemmaWords = searchLemmas.stream().map(Lemma::getLemmaWord).toList();
		return highlightUnformattedSnippet(formattedContent, lemmaWords);
	}

	private static String substringAroundLemma(String formattedSnippet, List<Lemma> searchLemmas) {
		String lemmaWord = searchLemmas.get(0).getLemmaWord();
		int[] substringIndexes = defineStartAndEndIndexes(formattedSnippet, lemmaWord);
		int startIndex = substringIndexes[0];
		int endIndex = substringIndexes[1];
		formattedSnippet = formattedSnippet.replaceAll("[^А-яёЁ0-9-:;.,\\s]+", "").replaceAll("\\s+", " ");
		formattedSnippet = formattedSnippet.substring(startIndex, endIndex).replaceAll("\\s+", " ").trim();
		formattedSnippet = formattedSnippet.substring(formattedSnippet.indexOf(" "), formattedSnippet.lastIndexOf(" "));
		return formattedSnippet;
	}

	private static int[] defineStartAndEndIndexes(String snippet, String lemmaWord) {
//	lemmatize snippet
		String formattedSnippet = morphologyUtils.lemmatizeSnippet(snippet);
		int startIndex = formattedSnippet.indexOf(lemmaWord) - 150;
		int lengthChangeCounter = 0;
		while (startIndex < 0) {
			startIndex += 10;
			lengthChangeCounter++;
		}
		int endIndex = formattedSnippet.indexOf(lemmaWord) + lemmaWord.length() + 151;
		if (endIndex < formattedSnippet.length()) {
			for (int i = 0; i < lengthChangeCounter; i++) {
				endIndex += 10;
			}
		}
		while (endIndex > formattedSnippet.length()) {
			endIndex -= 10;
		}

		String beforeStartSnippet = snippet.substring(0, startIndex);
		int symbolsBeforeStart = countSymbols(beforeStartSnippet);
		String atSnippet = snippet.substring(startIndex, endIndex);
		int symbolsAtSnippet = countSymbols(atSnippet);

		startIndex += symbolsBeforeStart + symbolsAtSnippet;
		endIndex += symbolsBeforeStart + symbolsAtSnippet;
		return new int[]{startIndex, endIndex};
	}

	private static int countSymbols(String snippet) {
		int symbolsCount = 0;
		if (!snippet.isEmpty()) {
			symbolsCount = (int) snippet.chars().mapToObj(charAsInt -> String.valueOf((char) charAsInt)).filter(charAsStr -> {
				String pattern = "[0-9-,.;:]";
				Pattern compiledPattern = Pattern.compile(pattern);
				Matcher matcher = compiledPattern.matcher(charAsStr);
				return matcher.find();
			}).count();
		}
		return symbolsCount;
	}

	private static String highlightUnformattedSnippet(String snippet, List<String> lemmaWords) {
		//match lemma in normalSnippet
		StringBuilder builder = new StringBuilder();
		snippet = snippet.replaceAll("[A-z<>=\"]+", "");
		String[] unformattedSpl = snippet.split("\\s+");
		for (String str : unformattedSpl) {
			String lemmatizedStr = morphologyUtils.lemmatizeSnippet(str);
			if (lemmatizedStr.isEmpty()) continue;
//			boolean wasHighlighted = false;
			StringBuilder strBuilder = new StringBuilder(str);
			for (String lemma : lemmaWords) {
				if (lemmatizedStr.contains(lemma) /*&& !wasHighlighted*/) {
					strBuilder = new StringBuilder("<b>" + strBuilder + "</b>");
//					wasHighlighted = true;
				}
			}
			builder.append(" ").append(strBuilder);
		}
		return builder.toString();
	}

	public static String formatTitle(String pageContent) {
		pageContent = pageContent.replaceAll("<title[A-zА-я0-9-,.'\"()\\s=]>", "");
		return pageContent.substring(pageContent.indexOf("<title>") + 7, pageContent.indexOf("</title>"));
	}
}
