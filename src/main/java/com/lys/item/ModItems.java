package com.lys.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;
import com.lys.Shit;

public class ModItems {
    public static final Item EXAMPLE_ITEM = new Item(new FabricItemSettings());
    public static final Item JET_ITEM = new JetItem();

    public static void registerModItems() {
        Registry.register(Registry.ITEM, new Identifier(Shit.MOD_ID, "example_item"), EXAMPLE_ITEM);
        Registry.register(Registry.ITEM, new Identifier(Shit.MOD_ID, "jet_item"), JET_ITEM);
    }
}
