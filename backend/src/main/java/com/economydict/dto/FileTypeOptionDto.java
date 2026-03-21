package com.economydict.dto;

public class FileTypeOptionDto {
    private String code;
    private String displayName;

    public FileTypeOptionDto() {
    }

    public FileTypeOptionDto(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
