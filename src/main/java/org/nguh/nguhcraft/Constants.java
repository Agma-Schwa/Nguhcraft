package org.nguh.nguhcraft;

public interface Constants {
    /** Limit at which the anvil screen is disabled. */
    int ANVIL_LIMIT = 10000;

    /** Maximum distance to the target after which we give up. */
    int MAX_HOMING_DISTANCE = 128;

    /** Maximum enchantment level. */
    int MAX_ENCHANT_LVL = 255;

    /** Colours. */
    int Lavender = 0xC8BFE7;
    int Green = 0x55FF55;
    int Blue = 0x5555FF;
    int Grey = 0xAAAAAA;
    int DeepKoamaru = 0x332563;
    int White = 0xFFFFFF;
    int Red = 0xFF5555;
    int Gold = 0xFFAA00;
    int Orange = 16753920;
    int Black = 0;
    int Purple = 0xAA00AA;

    /**
    * Used to represent ‘infinity’.
    * <p>
    * Actual Infinity is a very problematic value in that e.g. multiplying
    * it by 0 produces NaN, and so on. To avoid that, we use a very large
    * value instead.
    */
    float BIG_VALUE_FLOAT = 1e15f;
    double BIG_VALUE = 1e15;
}
