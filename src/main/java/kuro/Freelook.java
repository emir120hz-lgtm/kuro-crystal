package kuro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Freelook: karakterin yonu sabit kalir, sadece kamera doner.
 * Kapatinca kamera karakterin yonune yumusakca geri kayar.
 */
public final class Freelook {
   public static boolean active;
   public static float yaw;
   public static float pitch;
   /** 0 = karakterin bakisi, 1 = freelook bakisi */
   private static float blend;
   private static long last = System.nanoTime();

   private Freelook() {
   }

   public static void enable() {
      ClientPlayerEntity p = MinecraftClient.getInstance().player;
      if (p != null) {
         yaw = p.getYaw();
         pitch = p.getPitch();
      }

      active = true;
   }

   public static void disable() {
      active = false;
   }

   public static void look(double dx, double dy) {
      yaw += (float)dx * 0.15F;
      pitch = MathHelper.clamp(pitch + (float)dy * 0.15F, -90.0F, 90.0F);
   }

   /** Kamera guncellenmeden hemen once cagrilir: gecisi (blend) ilerletir. */
   public static void updateBlend() {
      long now = System.nanoTime();
      float dt = Math.min(0.1F, (now - last) / 1.0E9F);
      last = now;
      float target = active ? 1.0F : 0.0F;
      blend += (target - blend) * (1.0F - (float)Math.exp(-16.0 * dt));
      if (Math.abs(target - blend) < 0.0005F) {
         blend = target;
      }
   }

   public static boolean applies() {
      return active || blend > 0.0F;
   }

   public static float camYaw(float playerYaw) {
      return playerYaw + MathHelper.wrapDegrees(yaw - playerYaw) * blend;
   }

   public static float camPitch(float playerPitch) {
      return playerPitch + (pitch - playerPitch) * blend;
   }
}
