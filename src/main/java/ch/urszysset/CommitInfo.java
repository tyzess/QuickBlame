package ch.urszysset;

import org.apache.commons.lang.StringUtils;

public class CommitInfo {

    private String author;
    private String email;
    private String date;
    private String code;
    private int startLineNumber;

    public CommitInfo(int startLineNumber) {
        this.startLineNumber = startLineNumber;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getStartLineNumber() {
        return startLineNumber;
    }

    public boolean isOneLineCode() {
        return code == null || StringUtils.countMatches(code, "\n") < 1;
    }
}
