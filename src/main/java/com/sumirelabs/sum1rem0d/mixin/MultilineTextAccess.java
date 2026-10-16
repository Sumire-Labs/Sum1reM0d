package com.sumirelabs.sum1rem0d.mixin;
import net.minecraft.client.gui.components.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MultiLineEditBox.class)
public interface MultilineTextAccess {@Accessor("textField") MultilineTextField sum1rem0d$text();}
