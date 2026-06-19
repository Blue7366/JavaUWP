package net.minecraftforge.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    String value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface EventBusSubscriber {
        String modid();

        Bus bus() default Bus.MOD;

        net.minecraftforge.api.distmarker.Dist[] value() default {};

        enum Bus {
            MOD,
            FORGE,
            GAME
        }
    }
}
