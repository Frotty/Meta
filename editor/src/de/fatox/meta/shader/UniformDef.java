package de.fatox.meta.shader;

/**
 * Created by Frotty on 29.06.2016.
 */
public class UniformDef {
    public final String name;
    public final int location;

    public UniformDef(String name, int location) {
        this.name = name;
        this.location = location;
    }
}
