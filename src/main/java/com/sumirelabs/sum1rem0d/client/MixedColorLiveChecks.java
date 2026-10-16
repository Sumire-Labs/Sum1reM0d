package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.game.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.*;

final class MixedColorLiveChecks {
 private static int ticks;
 static boolean tick(Minecraft mc,Consumer<String> passed)throws Exception{
  if(ticks<0)return true;
  if(++ticks<60)return false;
  if(ticks==60 && mc.gui.screen() instanceof WikiScreen)Screenshot.grab(mc.gameDirectory,"sumire-wiki-icon.png",mc.gameRenderer.mainRenderTarget(),1,message->{});
  if(ticks==60){mc.gui.setScreen(null);mc.gui.toastManager().clear();mc.player.setPos(0.5,247,8.5);mc.player.setYRot(180);mc.player.setXRot(50);return false;}
  if(ticks<80)return false;
  for(int i=0;i<3;i++){
   var pos=MixedColorFixture.position(i);var state=mc.level.getBlockState(pos);
   if(!(mc.level.getBlockEntity(pos) instanceof DyedBlockEntity entity)||entity.color()!=MixedColorFixture.RGB)throw new AssertionError("Placed block RGB reached client "+i);
   var tint=mc.getBlockColors().getTintSource(state,0);
   if(tint==null||tint.colorInWorld(state,mc.level,pos)!=(0xFF000000|MixedColorFixture.RGB))throw new AssertionError("Placed block tint "+i);
   try{
    var extension=Class.forName("net.caffeinemc.mods.sodium.client.model.color.interop.BlockColorsExtension");
    var providers=(Map<?,?>)extension.getMethod("sodium$getProviders").invoke(mc.getBlockColors());
    if(!providers.containsKey(state.getBlock()))throw new AssertionError("Sodium registered tint "+i);
   }catch(ClassNotFoundException absent){/* Vanilla renderer control run. */}
  }
  // Both bed halves must receive the component, including the head placed by vanilla.
  var head=MixedColorFixture.position(2).south();
  if(!(mc.level.getBlockEntity(head) instanceof DyedBlockEntity bed)||bed.color()!=MixedColorFixture.RGB)throw new AssertionError("Bed head RGB reaches client");
  mc.gui.setScreen(null);
  Screenshot.grab(mc.gameDirectory,"sumire-mixed-world.png",mc.gameRenderer.mainRenderTarget(),1,message->{});
  passed.accept("Real BlockItem placements synchronize concrete, wool and both bed halves; actual renderer tint registration includes Sodium when installed");
  ticks=-1;return true;
 }
}
