package mopsy.productions.nexo.REICompat;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REIServerPlugin;
import mopsy.productions.nexo.REICompat.categories.air_separator.AirSeparatorDisplay;
import mopsy.productions.nexo.REICompat.categories.centrifuge.CentrifugeDisplay;
import mopsy.productions.nexo.REICompat.categories.crusher.CrushingDisplay;
import mopsy.productions.nexo.REICompat.categories.electrolyzer.ElectrolyzerDisplay;
import mopsy.productions.nexo.REICompat.categories.filling.FillingDisplay;
import mopsy.productions.nexo.REICompat.categories.mixer.MixerDisplay;
import mopsy.productions.nexo.REICompat.categories.press.PressDisplay;

import static mopsy.productions.nexo.Main.modid;

public class REIServerCompat implements REIServerPlugin {
    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(CategoryIdentifier.of(modid,"crusher"), CrushingDisplay.SERIALIZER);
        registry.register(CategoryIdentifier.of(modid,"press"), PressDisplay.SERIALIZER);
        registry.register(CategoryIdentifier.of(modid,"mixer"), MixerDisplay.SERIALIZER);
        registry.register(CategoryIdentifier.of(modid,"centrifuge"), CentrifugeDisplay.SERIALIZER);
        registry.register(CategoryIdentifier.of(modid,"electrolyzer"), ElectrolyzerDisplay.SERIALIZER);
        registry.register(CategoryIdentifier.of(modid,"air_separator"), AirSeparatorDisplay.SERIALIZER);
        registry.register(CategoryIdentifier.of(modid,"filling"), FillingDisplay.SERIALIZER);
    }
}
