package org.ih.health.record.exchange.domain;

import java.util.*;

public class ObservationDTO {

	private String title;
	private ArrayList<String> data = new ArrayList<>();


	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ArrayList<String> getData() {
		return data;
	}

	public void setData(ArrayList<String> data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "Observation [title=" + title + ", data=" + data + "]";
	}

}
