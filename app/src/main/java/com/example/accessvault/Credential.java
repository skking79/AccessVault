package com.example.accessvault;

public class Credential {
    private long id;
    private String siteName;
    private String username;
    private String password;
    public Credential(long id, String siteName, String username, String password) {
        this.id = id;
        this.siteName = siteName;
        this.username = username;
        this.password = password;
    }
    public Credential(String siteName, String username, String password) {
        this(-1, siteName, username, password);
    }
    public long getId() {
        return id;
    }
    public String getSiteName() {
        return siteName;
    }
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Credential that = (Credential) o;
        return id == that.id;
    }
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}