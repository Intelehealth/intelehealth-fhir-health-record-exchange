package org.ih.health.record.exchange.domain;

import java.util.*;

public class ObservationDTO {

	private String title;
	private Collection<ArrayList> data = new ArrayList<>();


	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Collection<ArrayList> getData() {
		return data;
	}

	public void setData(Collection<ArrayList> collection) {
		this.data = collection;
	}

	@Override
	public String toString() {
		return "Observation [title=" + title + ", data=" + data + "]";
	}

}
