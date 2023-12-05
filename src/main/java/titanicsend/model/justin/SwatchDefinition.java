package titanicsend.model.justin;

public class SwatchDefinition {

    public final String label;
    public final int index;

    public SwatchDefinition(String label, int index) {
        this.label = label;
        this.index = index;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
