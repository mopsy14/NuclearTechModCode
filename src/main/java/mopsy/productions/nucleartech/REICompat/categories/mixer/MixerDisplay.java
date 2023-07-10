package mopsy.productions.nucleartech.REICompat.categories.mixer;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import mopsy.productions.nucleartech.REICompat.NEXODisplay;
import mopsy.productions.nucleartech.recipes.MixerRecipe;
import mopsy.productions.nucleartech.recipes.NEXORecipe;

import static mopsy.productions.nucleartech.Main.modid;

@SuppressWarnings("UnstableApiUsage")
public class MixerDisplay extends NEXODisplay {

    public MixerRecipe recipe;

    public MixerDisplay(NEXORecipe recipe) {
        super(recipe);
    }
    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CategoryIdentifier.of(modid,"mixer");
    }
}
