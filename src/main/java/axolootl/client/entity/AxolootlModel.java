package axolootl.client.entity;

import axolootl.entity.AxolootlEntity;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.geom.ModelPart;

public class AxolootlModel<T extends AxolootlEntity> extends AxolotlModel<T> {

    private static final Vector3f ONE = new Vector3f(1, 1, 1);

    protected Vector3f primaryColor;
    protected Vector3f secondaryColor;

    public AxolootlModel(ModelPart pRoot) {
        super(pRoot);
        this.primaryColor = ONE;
        this.secondaryColor = ONE;
    }

    public void setColors(Vector3f primaryColor, Vector3f secondaryColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    public void resetColors() {
        this.primaryColor = ONE;
        this.secondaryColor = ONE;
    }
}
