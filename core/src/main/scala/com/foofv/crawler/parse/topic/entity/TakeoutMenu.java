package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "TakeoutMenu")
public class TakeoutMenu extends Menu {
	
	public static void main(String[] args) {
		TakeoutMenu tm = new TakeoutMenu();
		tm.setMenuName("test");
		System.out.println(tm);
	}

}
