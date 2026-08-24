package com.securefilesharing.dto;

public class FileResponse {
	
    private Long id;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private Long uploadedBy;
	public FileResponse() {
		super();
	}
	public FileResponse(Long id, String originalFileName, String contentType, Long fileSize, Long uploadedBy) {
		super();
		this.id = id;
		this.originalFileName = originalFileName;
		this.contentType = contentType;
		this.fileSize = fileSize;
		this.uploadedBy = uploadedBy;
	}
	public Long getId() {
		return id;
	}
	public String getOriginalFileName() {
		return originalFileName;
	}
	public String getContentType() {
		return contentType;
	}
	public Long getFileSize() {
		return fileSize;
	}
	public Long getUploadedBy() {
		return uploadedBy;
	}
    
    
	
	
}
