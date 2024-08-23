package ru.skillbox.bogdan_imankulov.search_engine.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {

	private boolean result;
	private int count;
	private List<SearchData> data;
	private String error = null;

	public SearchResponse(boolean result, int count, List<SearchData> data) {
		this.result = result;
		this.count = count;
		this.data = data;
	}

	public SearchResponse(boolean result, int count, List<SearchData> data, String error) {
		this.result = result;
		this.count = count;
		this.data = data;
		this.error = error;
	}
}
