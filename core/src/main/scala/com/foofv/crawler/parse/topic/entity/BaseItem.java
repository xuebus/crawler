package com.foofv.crawler.parse.topic.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.mongodb.morphia.annotations.Id;

public abstract class BaseItem {

	/**
	 * objectId, mongodb
	 */
	@Id
	private String objectId = java.util.UUID.randomUUID().toString();
	/**
	 * data job id
	 */
	private long jobId = -1;
	
	/**
	 * data job name 
	 */
	private String jobName = "";

	/**
	 * data batch id
	 */
	private int batchId = -1;

	/**
	 * data task id
	 */
	private String taskId = "";
	
	/**
	 * origin site domain
	 */
	private String originSite = "";
	
	private Long createDate = System.currentTimeMillis();
	private Long updateDate = System.currentTimeMillis();

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public int getBatchId() {
		return batchId;
	}

	public void setBatchId(int batchId) {
		this.batchId = batchId;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getOriginSite() {
		return originSite;
	}

	public void setOriginSite(String originSite) {
		this.originSite = originSite;
	}

	public Long getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Long createDate) {
		this.createDate = createDate;
	}

	public Long getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Long updateDate) {
		this.updateDate = updateDate;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}

}
