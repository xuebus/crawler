package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Reference;

import java.util.List;

public abstract class Menu extends BaseItem {

	private java.lang.String merchantId = "";
	private java.lang.String siteMenuId = "";
	private java.lang.String menuName = "";
	private java.lang.String menuDescription = "";
	@Reference
	private List<Food> foods;

	public java.lang.String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(java.lang.String merchantId) {
		this.merchantId = merchantId;
	}

	public java.lang.String getSiteMenuId() {
		return siteMenuId;
	}

	public void setSiteMenuId(java.lang.String siteMenuId) {
		this.siteMenuId = siteMenuId;
	}

	public java.lang.String getMenuName() {
		return menuName;
	}

	public void setMenuName(java.lang.String menuName) {
		this.menuName = menuName;
	}

	public java.lang.String getMenuDescription() {
		return menuDescription;
	}

	public void setMenuDescription(java.lang.String menuDescription) {
		this.menuDescription = menuDescription;
	}

	public List<Food> getFoods() {
		return foods;
	}

	public void setFoods(List<Food> foods) {
		this.foods = foods;
	}

}
