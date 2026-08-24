package com.securefilesharing.dto;

public class LoginResponse {
	
	private String token;
	private String name;

	public LoginResponse() {
		super();
	}

	public LoginResponse(String token, String name) {
		super();
		this.token = token;
		this.name = name;
	}

	public String getToken() {
		return token;
	}

	public String getName() {
		return name;
	}
	
	
}
