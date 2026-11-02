package com.glory.menu.mixin;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** setHeight is not public in every version, the field is. */
@Mixin(ClickableWidget.class)
public interface WidgetAccessor {
    @Mutable @Accessor("height")
    void glory$setHeight(int h);
}
