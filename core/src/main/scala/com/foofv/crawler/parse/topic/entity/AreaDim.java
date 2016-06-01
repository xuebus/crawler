package com.foofv.crawler.parse.topic.entity;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "AreaDim")
public class AreaDim extends BaseItem {

	/**
	 * default ""
	 */
	private String name = "";

	/**
	 * default 1, top level
	 */
	private int level = 1;

	/**
	 * default "", no parent
	 */
	private String parent_id = "";
	
	/**
	 * default "", no parent
	 */
	private String parent_name = "";

	/**
	 * default "", code ids
	 */
	private String codes = "";

	/**
	 * default "", AreaDimension type
	 */
	private String type = "";

	/**
	 * default ""
	 */
	private String extra_type = "";

	/**
	 * default ""
	 */
	private String codes_default = "";

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getParent_id() {
		return parent_id;
	}

	public void setParent_id(String parent_id) {
		this.parent_id = parent_id;
	}

	public String getParent_name() {
		return parent_name;
	}

	public void setParent_name(String parent_name) {
		this.parent_name = parent_name;
	}

	public String getCodes() {
		return codes;
	}

	public void setCodes(String codes) {
		this.codes = codes;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getExtra_type() {
		return extra_type;
	}

	public void setExtra_type(String extra_type) {
		this.extra_type = extra_type;
	}

	public String getCodes_default() {
		return codes_default;
	}

	public void setCodes_default(String codes_default) {
		this.codes_default = codes_default;
	}

	public static void main(String[] args) {
		AreaDim area = new AreaDim();
		area.setType("city");
		System.out.println(area);
	}

}
