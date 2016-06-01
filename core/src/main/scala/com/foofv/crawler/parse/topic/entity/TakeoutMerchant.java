package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "TakeoutMerchant")
public class TakeoutMerchant extends RestrautMerchant {

	
	private String soldTotal = "";
	private String intimeRate = "";
	private String deliveryStartPrice = "";
	private String sendPrice = "";
	private String averageSendTime = "";
	private String deliveryProvider = "";
	private String discountCompaign = "";
	private String invoiceSupported = "";
	private String onlinepaySupported = "";
	private String saveUpCount = "";

	
	public String getSoldTotal() {
		return soldTotal;
	}

	public void setSoldTotal(String soldTotal) {
		this.soldTotal = soldTotal;
	}

	public String getIntimeRate() {
		return intimeRate;
	}

	public void setIntimeRate(String intimeRate) {
		this.intimeRate = intimeRate;
	}

	public String getDeliveryStartPrice() {
		return deliveryStartPrice;
	}

	public void setDeliveryStartPrice(String deliveryStartPrice) {
		this.deliveryStartPrice = deliveryStartPrice;
	}

	public String getSendPrice() {
		return sendPrice;
	}

	public void setSendPrice(String sendPrice) {
		this.sendPrice = sendPrice;
	}

	public String getAverageSendTime() {
		return averageSendTime;
	}

	public void setAverageSendTime(String averageSendTime) {
		this.averageSendTime = averageSendTime;
	}

	public String getDeliveryProvider() {
		return deliveryProvider;
	}

	public void setDeliveryProvider(String deliveryProvider) {
		this.deliveryProvider = deliveryProvider;
	}

	public String getDiscountCompaign() {
		return discountCompaign;
	}

	public void setDiscountCompaign(String discountCompaign) {
		this.discountCompaign = discountCompaign;
	}

	public String getInvoiceSupported() {
		return invoiceSupported;
	}

	public void setInvoiceSupported(String invoiceSupported) {
		this.invoiceSupported = invoiceSupported;
	}

	public String getOnlinepaySupported() {
		return onlinepaySupported;
	}

	public void setOnlinepaySupported(String onlinepaySupported) {
		this.onlinepaySupported = onlinepaySupported;
	}

	public String getSaveUpCount() {
		return saveUpCount;
	}

	public void setSaveUpCount(String saveUpCount) {
		this.saveUpCount = saveUpCount;
	}

}
