package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "SpecialityMenu")
public class SpecialityMenu extends Menu {
	
	public static void main(String[] args) {
		SpecialityMenu tm = new SpecialityMenu();
		tm.setMenuName("test");
		System.out.println(tm);
	}

}
