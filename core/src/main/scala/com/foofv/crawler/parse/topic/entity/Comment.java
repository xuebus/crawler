package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "Comment")
public class Comment extends BaseItem {

	private String commentId = "";
	private String merchantId = "";
	private String userId = "";
	private String username = "";
	private String commentContent = "";
	private String commentTime = "";
	private String commentScore = "";
	private String hasContent = "";

	public String getCommentId() {
		return commentId;
	}

	public void setCommentId(String commentId) {
		this.commentId = commentId;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCommentContent() {
		return commentContent;
	}

	public void setCommentContent(String commentContent) {
		this.commentContent = commentContent;
		this.hasContent = "1";
	}

	public String getCommentTime() {
		return commentTime;
	}

	public void setCommentTime(String commentTime) {
		this.commentTime = commentTime;
	}

	public String getCommentScore() {
		return commentScore;
	}

	public void setCommentScore(String commentScore) {
		this.commentScore = commentScore;
	}

	public String isHasContent() {
		return hasContent;
	}

	public void setHasContent(String hasContent) {
		this.hasContent = hasContent;
	}

	public static void main(String[] args) {
		Comment cmt = new Comment();
		cmt.setCommentContent("test test");
		System.out.println(cmt);
	}

}
