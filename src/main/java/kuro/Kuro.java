package kuro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import kuro.ui.Hud;
import kuro.ui.ClickGuiScreen;
import kuro.ui.MenuScreen;
import kuro.mixin.MinecraftClientAccessor;
import kuro.ui.Toasts;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Kuro implements ClientModInitializer {
   public static final Logger LOGGER = LoggerFactory.getLogger("kuro");
   public static final MinecraftClient mc = MinecraftClient.getInstance();

   // Modul indeksleri (Mods / Render bunlari kullanir)
   public static final int FLY = 0;
   public static final int TOTEM = 1;
   public static final int CRYSTAL = 2;
   public static final int STORAGE = 3;
   public static final int BLOCK = 4;
   public static final int PLAYER = 5;
   public static final int BASES = 6; // aksiyon: acik/kapali durumu yok
   public static final int FREECAM = 7;
   public static final int FREELOOK = 8;
   public static final int CMACRO = 9;
   public static final int AMACRO = 10;
   public static final int SPRINT = 11;
   public static final int NOFALL = 12;
   public static final int SPEED = 13;
   public static final int FULLBRIGHT = 14;
   public static final int KILLAURA = 15;
   public static final int TRIGGERBOT = 16;
   public static final int VELOCITY = 17;
   public static final int FASTPLACE = 18;
   public static final int SAFEANCHOR = 19;
   public static final int COBWEB = 20;
   public static final int CRYSTALOPT = 21;
   public static final int ELYTRASWAP = 22; // aksiyon
   public static final int HITBOXES = 23;
   public static final int JUMPRESET = 24;
   public static final int MISPLACE = 25;
   public static final int REACH = 26;
   public static final int SHIELDBREAKER = 27;
   public static final int VIEWMODEL = 28;
   public static final int SWINGANIM = 29;
   public static final int NOHURTCAM = 30;
   public static final int WORLDTIME = 31;
   public static final int AUTOCLICKER = 32;
   public static final int AUTOTOOL = 33;
   public static final int AUTOEAT = 34;
   public static final int CHESTSTEALER = 35;
   public static final int AUTOARMOR = 36;
   public static final int AUTORESPAWN = 37;
   public static final int AUTOLOG = 38;
   public static final int STEP = 39;
   public static final int ZOOM = 40;
   public static final int ITEMESP = 41;
   public static final int TARGETHUD = 42;
   public static final int NOBOB = 43;
   public static final int CRYSTALBASE = 44;
   public static final int NAMETAGS = 45;

   public static final String[] NAMES = new String[]{
      "Fly", "AutoTotem", "AutoCrystal", "StorageESP", "BlockESP", "PlayerESP", "BaseList", "Freecam", "Freelook", "CrystalMacro", "AnchorMacro",
      "Sprint", "NoFall", "Speed", "Fullbright", "KillAura", "TriggerBot", "Velocity", "FastPlace", "SafeAnchor",
      "CobWebPlacer", "CrystalOptimizer", "ElytraSwap", "Hitboxes", "JumpReset", "Misplace", "Reach", "ShieldBreaker",
      "ViewModel", "SwingAnim", "NoHurtCam", "WorldTime",
      "AutoClicker", "AutoTool", "AutoEat", "ChestStealer", "AutoArmor", "AutoRespawn", "AutoLog", "Step",
      "Zoom", "ItemESP", "TargetHUD", "NoBob",
      "BaseAura", "Nametags"
   };
   public static final String[] DESCS = new String[]{
      "Havada uçma. WASD, boşluk ve shift ile yön ver",
      "Totemi otomatik olarak sol ele alır",
      "Yakındaki oyunculara otomatik kristal koyar ve patlatır",
      "Sandık, varil, shulker, huni ve fırın gibi depoları çerçeveler",
      "kuro_blocks.txt içindeki blokları çerçeveler",
      "Oyuncuları kutu ve çizgiyle gösterir",
      "Depo yoğunluğuna göre olası base konumlarını listeler",
      "Kamerayı karakterden ayırıp serbestçe uçurur",
      "Karakter yönü sabit kalırken etrafına bakmanı sağlar",
      "Sağ tık basılıyken obsidyene kristal koyar, kristale bakınca kırar",
      "Sağ tık basılıyken anchor'ı glowstone ile doldurup patlatır",
      "İleri giderken otomatik koşar",
      "Düşme hasarını engeller",
      "Yürüme hızını artırır",
      "Karanlıkta bile her yeri aydınlık gösterir",
      "Menzildeki oyuncuya yönelip saldırı bekleme süresi dolunca vurur",
      "Tam nişan aldığın oyuncuya otomatik vurur",
      "Aldığın itmeyi (knockback) engeller veya azaltır",
      "Blok koyma / eşya kullanma gecikmesini kaldırır",
      "Yakındaki respawn anchor ile senin aranda engel yoksa, aranıza (önüne) glowstone koyup patlamayı engeller; hotbar'da glowstone gerekir",
      "Sağ tık basılıyken en yakın oyuncunun ayağına nişan alıp cobweb koyar; hotbar'da cobweb gerekir",
      "Kristale vurur vurmaz onu istemcide kaldırır, böylece yenisini daha hızlı koyabilirsin",
      "Tuşa basınca giydiğin elytra ile göğüslüğü değiştirir",
      "Diğer oyuncuların vuruş kutusunu çok az büyütür",
      "Vurulunca rastgele gecikmeyle zıplayarak itmeyi azaltır",
      "Sağ tık basılıyken nişan oyuncunun üstündeyse, arkasındaki obsidyene kristal koyar",
      "Saldırı menzilini çok az uzatır",
      "Kalkan tutan oyuncuya bakınca baltaya geçip vurur, sonra eski slota döner",
      "Elin konumunu ve boyutunu ayarlar",
      "El sallama stilini, hızını ve eşya değiştirme animasyonunu değiştirir",
      "Hasar alınca kameranın sallanmasını kapatır",
      "Oyun içi saati sabitler, istersen yağmuru kapatır (sadece görüntü)",
      "Sol tık basılıyken, nişan bir varlıktayken rastgele CPS ile vurur",
      "Blok kırarken hotbar'daki en hızlı aleti seçer, bırakınca geri döner",
      "Açlık eşiğin altına düşünce hotbar'daki yemeği yer",
      "Sandık açılınca eşyaları rastgele gecikmeyle envanterine alır",
      "Boş zırh slotlarını envanterdeki zırhla doldurur",
      "Ölünce rastgele gecikmeyle otomatik yeniden doğar",
      "Canın eşiğin altına düşünce sunucudan çıkar",
      "Blok çıkma yüksekliğini artırır",
      "Görüş açısını (FOV) daraltarak yakınlaştırır",
      "Yere düşen eşyaları kutuyla gösterir",
      "Nişan aldığın oyuncunun adı ve canını gösterir",
      "Yürürken kamera sallanmasını kapatır",
      "Hedefin yanında uygun nokta yoksa obsidyen koyar, üstüne kristal koyup patlatır; duvar arkası seçeneği var",
      "Oyuncuların üstünde can, mesafe, ping ve zırh bilgisi gösterir (duvarın arkasından da görünür)"
   };
   public static final boolean[] on = new boolean[NAMES.length];
   public static final int[] binds = new int[]{71, 90, 67, 86, 66, 78, 72, 74, 76, 82, 75, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
   static final boolean[] prev = new boolean[NAMES.length];
   public static KeyBinding menuKey;

   public void onInitializeClient() {
      Config.load();
      loadBinds();
      menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.kuro.menu", InputUtil.Type.KEYSYM, 344, "Kuro"));

      ClientTickEvents.END_CLIENT_TICK.register(c -> {
         if (c.player == null || c.world == null) {
            Visuals.zoomTick();
         Visuals.bobTick();
         // dunyadan cikildi: kamera modlarini kapat
            if (on[FREECAM] || on[FREELOOK]) {
               setOn(FREECAM, false);
               setOn(FREELOOK, false);
            }

            return;
         }

         while (menuKey.wasPressed()) {
            if (c.currentScreen == null) {
               c.setScreen(Config.columnMenu ? new ClickGuiScreen() : new MenuScreen());
            }
         }

         long win = c.getWindow().getHandle();
         for (int i = 0; i < binds.length; i++) {
            boolean down = binds[i] >= 0 && InputUtil.isKeyPressed(win, binds[i]);
            if (down && !prev[i] && c.currentScreen == null) {
               toggle(i);
            }

            prev[i] = down;
         }

         Hud.tick();
         Freecam.tick();

         if (on[FLY]) {
            Mods.fly();
         }

         if (on[TOTEM]) {
            Mods.totem();
         }

         if (on[CRYSTAL]) {
            if (Config.acLegit >= 0.5F) {
               AutoCrystal.tick();
            } else {
               Mods.crystal();
            }
         } else {
            AutoCrystal.reset();
         }

         if (on[CRYSTALBASE]) {
            BaseAura.tick();
         } else {
            BaseAura.reset();
         }

         if (on[CMACRO]) {
            Macros.crystal();
         }

         if (on[AMACRO]) {
            Macros.anchor();
         }

         if (on[SAFEANCHOR]) {
            SafeAnchor.tick();
         } else {
            SafeAnchor.reset();
         }

         if (on[SPRINT]) {
            Extras.sprint();
         }

         if (on[NOFALL]) {
            Extras.noFall();
         }

         if (on[SPEED]) {
            Extras.speed();
         }

         if (on[FULLBRIGHT]) {
            Extras.fullbright();
         }

         if (on[KILLAURA]) {
            Extras.killAura();
         }

         if (on[TRIGGERBOT]) {
            Extras.triggerBot();
         }

         if (on[COBWEB]) {
            Combat.cobweb();
         }

         if (on[MISPLACE]) {
            Combat.misplace();
         }

         if (on[SHIELDBREAKER]) {
            Combat.shieldBreaker();
         }

         if (on[JUMPRESET]) {
            Combat.jumpReset();
         } else {
            Combat.jumpResetOff();
         }

         Combat.reach();
         Combat.elytraTick();

         if (on[AUTOCLICKER]) {
            Auto.clicker();
         }

         if (on[AUTOTOOL]) {
            Auto.tool();
         } else {
            Auto.toolOff();
         }

         if (on[AUTOEAT]) {
            Auto.eat();
         } else {
            Auto.eatOff();
         }

         if (on[CHESTSTEALER]) {
            Auto.stealer();
         }

         if (on[AUTOARMOR]) {
            Auto.armor();
         }

         if (on[AUTORESPAWN]) {
            Auto.respawn();
         }

         if (on[AUTOLOG]) {
            Auto.log();
         }

         Auto.step();
         Visuals.zoomTick();
         Visuals.bobTick();

         if (on[WORLDTIME]) {
            Visuals.worldTime();
         }

         if (on[FASTPLACE]) {
            ((MinecraftClientAccessor)mc).kuro$setItemUseCooldown(0);
         }

         if (on[STORAGE] || on[BLOCK]) {
            Mods.scanTick();
         } else {
            Mods.clearScan();
         }
      });

      WorldRenderEvents.LAST.register(Render::render);
      HudRenderCallback.EVENT.register(Hud::render);
   }

   /** Aksiyon modulleri: acik/kapali durumu yok. */
   public static boolean isAction(int i) {
      return i == BASES || i == ELYTRASWAP;
   }

   /** Modulu ac/kapat (aksiyon modulleri calistirilir). */
   public static void toggle(int i) {
      if (i == BASES) {
         Mods.printBases();
         Toasts.push(NAMES[i], true, "Çalıştırıldı");
         return;
      }

      if (i == ELYTRASWAP) {
         boolean ok = Combat.elytraSwap();
         Toasts.push(NAMES[i], ok, ok ? "Çalıştırıldı" : "Eşya bulunamadı");
         return;
      }

      setOn(i, !on[i]);
      if (Config.toasts) {
         Toasts.push(NAMES[i], on[i], on[i] ? "Açıldı" : "Kapandı");
      } else if (mc.player != null) {
         mc.player.sendMessage(Text.literal("[Kuro] " + NAMES[i] + (on[i] ? " ACIK" : " KAPALI")), true);
      }
   }

   public static void setOn(int i, boolean v) {
      if (on[i] == v) {
         return;
      }

      on[i] = v;
      if (i == FREECAM) {
         if (v) {
            Freecam.enable();
         } else {
            Freecam.disable();
         }
      } else if (i == FULLBRIGHT) {
         if (!v) {
            Extras.fullbrightOff();
         }
      } else if (i == FREELOOK) {
         if (v) {
            Freelook.enable();
         } else {
            Freelook.disable();
         }
      }
   }

   static Path keysFile() {
      return FabricLoader.getInstance().getGameDir().resolve("kuro_keys.txt");
   }

   public static void saveBinds() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < binds.length; i++) {
         sb.append(NAMES[i]).append('=').append(binds[i]).append('\n');
      }

      try {
         Files.writeString(keysFile(), sb.toString());
      } catch (IOException ignored) {
      }
   }

   static void loadBinds() {
      Path f = keysFile();
      if (Files.exists(f)) {
         try {
            for (String line : Files.readAllLines(f)) {
               String[] p = line.trim().split("=");
               if (p.length == 2) {
                  for (int i = 0; i < NAMES.length; i++) {
                     if (NAMES[i].equals(p[0].trim())) {
                        binds[i] = Integer.parseInt(p[1].trim());
                     }
                  }
               }
            }
         } catch (NumberFormatException | IOException ignored) {
         }
      }
   }
}
