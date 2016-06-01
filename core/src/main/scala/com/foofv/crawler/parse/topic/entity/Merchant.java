package com.foofv.crawler.parse.topic.entity;

import java.util.List;

public abstract class Merchant extends BaseItem {

	private String merchantId = "";
	private String name = "";
	private String cityId = "";
	private String city = "";
	private String telnum = "";
	private String address = "";
	private String logoUrl = "";
	private String url = "";
	private String bulletin = "";
	private String rankScore = "";
	private String isOffShelf = "";
	private String merchantCategoryDimension = "";
	private String merchantCategoryDimensionId = "";
	private String extraServices = "";
	private String metaData = "";
	private String merchantIdRef = "";
	private String latitude = "";
	private String longitude = "";
	private String merchantAreaDimensionId = "";
	private List<Comment> comments;
	private String businessHours = "";
	private String merchantStyle = "";
	private String merchantDesc= "";

	public String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCityId() {
		return cityId;
	}

	public void setCityId(String cityId) {
		this.cityId = cityId;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getTelnum() {
		return telnum;
	}

	public void setTelnum(String telnum) {
		this.telnum = telnum;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getBulletin() {
		return bulletin;
	}

	public void setBulletin(String bulletin) {
		this.bulletin = bulletin;
	}

	public String getRankScore() {
		return rankScore;
	}

	public void setRankScore(String rankScore) {
		this.rankScore = rankScore;
	}

	public String getIsOffShelf() {
		return isOffShelf;
	}

	public void setIsOffShelf(String isOffShelf) {
		this.isOffShelf = isOffShelf;
	}

	public String getMerchantCategoryDimension() {
		return merchantCategoryDimension;
	}

	public void setMerchantCategoryDimension(String merchantCategoryDimension) {
		this.merchantCategoryDimension = merchantCategoryDimension;
	}

	public String getMerchantCategoryDimensionId() {
		return merchantCategoryDimensionId;
	}

	public void setMerchantCategoryDimensionId(
			String merchantCategoryDimensionId) {
		this.merchantCategoryDimensionId = merchantCategoryDimensionId;
	}

	public String getExtraServices() {
		return extraServices;
	}

	public void setExtraServices(String extraServices) {
		this.extraServices = extraServices;
	}

	public String getMetaData() {
		return metaData;
	}

	public void setMetaData(String metaData) {
		this.metaData = metaData;
	}

	public String getMerchantIdRef() {
		return merchantIdRef;
	}

	public void setMerchantIdRef(String merchantIdRef) {
		this.merchantIdRef = merchantIdRef;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getMerchantAreaDimensionId() {
		return merchantAreaDimensionId;
	}

	public void setMerchantAreaDimensionId(String merchantAreaDimensionId) {
		this.merchantAreaDimensionId = merchantAreaDimensionId;
	}

	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}
	
	public String getBusinessHours() {
		return businessHours;
	}

	public void setBusinessHours(String businessHours) {
		this.businessHours = businessHours;
	}

	public String getMerchantStyle() {
		return merchantStyle;
	}

	public void setMerchantStyle(String merchantStyle) {
		this.merchantStyle = merchantStyle;
	}

	public String getMerchantDesc() {
		return merchantDesc;
	}

	public void setMerchantDesc(String merchantDesc) {
		this.merchantDesc = merchantDesc;
	}

}
