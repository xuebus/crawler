package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "TakeoutFood")
public class TakeoutFood extends Food {

	public static void main(String[] args) {
		TakeoutFood tf = new TakeoutFood();
		System.out.println(tf);
	}

}
