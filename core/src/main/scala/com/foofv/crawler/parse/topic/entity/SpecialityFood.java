package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "SpecialityFood")
public class SpecialityFood extends Food {

	public static void main(String[] args) {
		SpecialityFood tf = new SpecialityFood();
		System.out.println(tf);
	}

}
