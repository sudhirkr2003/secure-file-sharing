package com.securefilesharing.dto;

public class UserResponse {
	
	private long id;
	private String name;
	private String email;
	public UserResponse() {
		super();
	}
	public UserResponse(long id, String name, String email) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
	}
	public long getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getEmail() {
		return email;
	}
	
	
}
