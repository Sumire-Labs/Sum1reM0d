package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
public abstract class ScribblingBookMixin {
 @Unique private int sum1rem0d$bookTicks;
 @Inject(method="tick",at=@At("TAIL"))
 private void sum1rem0d$type(CallbackInfo ci){
  if(!((Object)this instanceof BookEditAccess book) || !book.sum1rem0d$book().is(Items.WRITABLE_BOOK) || !Rules.on("scribbling_book"))return;
  if(++sum1rem0d$bookTicks<Math.max(1,Rules.i("scribbling_book","interval")/Rules.i("scribbling_book","speed_multiplier")))return;sum1rem0d$bookTicks=0;
  var page=book.sum1rem0d$page();if(page==null || page.getValue().length()>=1024)return;
  int[] chars=Rules.s("scribbling_book","text").codePoints().filter(c->!Character.isISOControl(c)).toArray();if(chars.length==0)return;
  var random=java.util.concurrent.ThreadLocalRandom.current();var text=new StringBuilder();int remaining=1024-page.getValue().length();
  for(int i=0;i<Rules.i("scribbling_book","characters");i++){int c=chars[random.nextInt(chars.length)];if(Character.charCount(c)>remaining)break;text.appendCodePoint(c);remaining-=Character.charCount(c);}
  var field=((MultilineTextAccess)page).sum1rem0d$text();field.setSelecting(false);field.seekCursor(Whence.ABSOLUTE,page.getValue().length());field.insertText(text.toString());
 }
}
