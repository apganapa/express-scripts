package com.express.utility.scripts;

public class UrlHrefPojo {

    private String href;

    private String pageUrl;

    private String uniqueAdobeDomain;

    private String uniqueNonAdobeDomain;

    public UrlHrefPojo(String href, String uniqueAdobeDomain, String uniqueNonAdobeDomain) {
        this.href = href;
        this.uniqueAdobeDomain = uniqueAdobeDomain;
        this.uniqueNonAdobeDomain = uniqueNonAdobeDomain;
    }

    public UrlHrefPojo(String pageUrl, String href) {
        this.pageUrl = pageUrl;
        this.href = href;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getUniqueAdobeDomain() {
        return uniqueAdobeDomain;
    }

    public void setUniqueAdobeDomain(String uniqueAdobeDomain) {
        this.uniqueAdobeDomain = uniqueAdobeDomain;
    }

    public String getUniqueNonAdobeDomain() {
        return uniqueNonAdobeDomain;
    }

    public void setUniqueNonAdobeDomain(String uniqueNonAdobeDomain) {
        this.uniqueNonAdobeDomain = uniqueNonAdobeDomain;
    }
}
