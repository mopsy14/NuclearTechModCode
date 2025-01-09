package mopsy.productions.nexo.REICompat.categories.crusher;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import mopsy.productions.nexo.REICompat.NEXODisplay;
import mopsy.productions.nexo.recipes.CrusherRecipe;
import mopsy.productions.nexo.recipes.NEXORecipe;

import static mopsy.productions.nexo.Main.modid;

public class CrushingDisplay extends NEXODisplay {

    public CrusherRecipe recipe;

    public CrushingDisplay(NEXORecipe recipe) {
        super(recipe);
        this.recipe= (CrusherRecipe) recipe;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CategoryIdentifier.of(modid,"crusher");
    }

    public static final DisplaySerializer<CrushingDisplay> SERIALIZER = buildSerializer(d->d.recipe,CrushingDisplay::new);
}
