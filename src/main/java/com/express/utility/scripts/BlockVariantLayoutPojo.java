package com.express.utility.scripts;

public class BlockVariantLayoutPojo {

    private String blockName;

    private String variantName;

    private String layoutName;

    private String pageUrl;

    private String language;

    private String lastModified;

    private int occurrence;

    public BlockVariantLayoutPojo(String blockName, String variantName, String layoutName, String pageUrl, String language, int occurrence, String lastModified) {
        this.blockName = blockName;
        this.variantName = variantName;
        this.layoutName = layoutName;
        this.pageUrl = pageUrl;
        this.language = language;
        this.occurrence = occurrence;
        this.lastModified = lastModified;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public String getLayoutName() {
        return layoutName;
    }

    public void setLayoutName(String layoutName) {
        this.layoutName = layoutName;
    }

    public int getOccurrence() {
        return occurrence;
    }

    public void setOccurrence(int occurrence) {
        this.occurrence = occurrence;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
}
