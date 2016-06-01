package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Reference;

import java.util.List;

public abstract class RestrautMerchant extends Merchant {

	@Reference
	private List<Menu> menus;

	public List<Menu> getMenus() {
		return menus;
	}

	public void setMenus(List<Menu> menus) {
		this.menus = menus;
	}

}
