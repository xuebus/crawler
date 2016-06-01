package com.foofv.crawler.storage;

import java.util.List;

public class MongoException extends Exception {

	private List<Object> list;
	private Exception exceptionInfo;

	public MongoException() {

	}

	public MongoException(List<Object> list, Exception exceptionInfo) {

		this.list = list;
		this.exceptionInfo = exceptionInfo;
	}

	public List<Object> getObjects() {
		return list;
	}
}
