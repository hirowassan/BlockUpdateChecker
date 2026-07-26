package xyz.crazyh.litemodtemplate;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.Tickable;
import net.minecraft.client.Minecraft;

import java.io.File;

/**
 * This is a very simple example LiteMod, it draws an analogue clock on the
 * minecraft HUD using a traditional onTick hook supplied by LiteLoader's
 * {@link Tickable} interface.
 *
 * @author Adam Mummery-Smith
 */

public class LiteModTemplate implements Tickable, LiteMod {

    /**
     * Called every frame
     *
     * @param minecraft    Minecraft instance
     * @param partialTicks Partial tick value
     * @param inGame       True if in-game, false if in the menu
     * @param clock        True if this is a new tick, otherwise false if it's a
     *                     regular frame
     */
    @Override
    public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock) {

    }

    /**
     * Get the mod version string
     *
     * @return the mod version as a string
     */
    @Override
    public String getVersion() {
        return null;
    }

    /**
     * Do startup stuff here, minecraft is not fully initialised when this
     * function is called so mods <b>must not</b> interact with minecraft in any
     * way here.
     *
     * @param configPath Configuration path to use
     */
    @Override
    public void init(File configPath) {

    }

    /**
     * Called when the loader detects that a version change has happened since
     * this mod was last loaded.
     *
     * @param version       new version
     * @param configPath    Path for the new version-specific config
     * @param oldConfigPath Path for the old version-specific config
     */
    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath) {

    }

    /**
     * Get the display name
     *
     * @return display name
     */
    @Override
    public String getName() {
        return null;
    }
}
