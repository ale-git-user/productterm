package com.termmed.model;

public class Substance {
    private String FSN;
    private String pref;
    private String conceptId;

    public String getFSN() {
        return FSN;
    }

    public void setFSN(String FSN) {
        this.FSN = FSN;
    }

    public String getPref() {
        return pref;
    }

    public void setPref(String pref) {
        this.pref = pref;
    }

    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public Substance(String conceptId, String fsn, String pref) {
        this.conceptId=conceptId;
        this.FSN=fsn;
        this.pref=pref;
    }
}
