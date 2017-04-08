package de.fatox.meta.api.dao;

/**
 * Created by Frotty on 08.04.2017.
 */
public class MetaTabData {
    public String tabName;
    public String tabClassName;
    public boolean displayed;

    public MetaTabData(String tabName, String tabClassName, boolean displayed) {
        this.tabName = tabName;
        this.tabClassName = tabClassName;
        this.displayed = displayed;
    }
}
