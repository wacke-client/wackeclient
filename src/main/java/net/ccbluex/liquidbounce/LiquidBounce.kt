/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce

import net.ccbluex.liquidbounce.discord.ClientRichPresence
import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.special.AntiForge
import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.script.remapper.Remapper.loadSrg
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.hud.HUD.Companion.createDefault
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.misc.sound.TipSoundManager
import net.ccbluex.liquidbounce.betterfps.BetterFPSCore
import net.minecraft.util.ResourceLocation
import kotlin.concurrent.thread

object LiquidBounce {

    // Client information
    const val CLIENT_NAME = "LiquidBounce+"
    const val CLIENT_VERSION = "101221-hotfix"
    const val CLIENT_CREATOR = "CCBlueX, WYSI-Foundation"
    const val CLIENT_CLOUD = "https://wysi-foundation.github.io/LiquidCloud/LiquidBounce"

    var isStarting = false
    var mainMenuPrep = false

    // Managers
    lateinit var moduleManager: ModuleManager
    lateinit var commandManager: CommandManager
    lateinit var eventManager: EventManager
    lateinit var fileManager: FileManager
    lateinit var scriptManager: ScriptManager

    var betterFPSCore: BetterFPSCore = BetterFPSCore()
    lateinit var tipSoundManager: TipSoundManager

    // HUD & ClickGUI
    lateinit var hud: HUD

    lateinit var clickGui: ClickGui

    // Menu Background
    var background: ResourceLocation? = null

    // Discord RPC
    lateinit var clientRichPresence: ClientRichPresence

    var lastTick : Long = 0L

    /**
     * Execute if client will be started
     */
    fun startClient() {
        isStarting = true

        ClientUtils.getLogger().info("Starting $CLIENT_NAME build $CLIENT_VERSION")
        lastTick = System.currentTimeMillis()

        // Create file manager
        fileManager = FileManager()

        // Crate event manager
        eventManager = EventManager()

        // Register listeners
        eventManager.registerListener(RotationUtils())
        eventManager.registerListener(AntiForge())
        eventManager.registerListener(BungeeCordSpoof())
        eventManager.registerListener(InventoryUtils())
        eventManager.registerListener(PacketUtils())

        // Init Discord RPC
        clientRichPresence = ClientRichPresence()

        // Create command manager
        commandManager = CommandManager()

        // Load client fonts
        Fonts.loadFonts()

        // Init SoundManager
        tipSoundManager = TipSoundManager()

        // Setup module manager and register modules
        moduleManager = ModuleManager()
        moduleManager.registerModules()

        // Remapper
        try {
            loadSrg()

            // ScriptManager
            scriptManager = ScriptManager()
            scriptManager.loadScripts()
            scriptManager.enableScripts()
        } catch (throwable: Throwable) {
            ClientUtils.getLogger().error("Failed to load scripts.", throwable)
        }

        // Register commands
        commandManager.registerCommands()

        // Load configs
        fileManager.loadConfigs(fileManager.modulesConfig, fileManager.valuesConfig, fileManager.accountsConfig,
                fileManager.friendsConfig, fileManager.xrayConfig)

        // ClickGUI
        clickGui = ClickGui()
        fileManager.loadConfig(fileManager.clickGuiConfig)

        // Set HUD
        hud = createDefault()
        fileManager.loadConfig(fileManager.hudConfig)

        // Disable optifine fastrender
        //ClientUtils.disableFastRender()

        // Load generators
        GuiAltManager.loadGenerators()

        // Setup Discord RPC
        if (clientRichPresence.showRichPresenceValue) {
            thread {
                try {
                    clientRichPresence.setup()
                } catch (throwable: Throwable) {
                    ClientUtils.getLogger().error("Failed to setup Discord RPC.", throwable)
                }
            }
        }

        ClientUtils.getLogger().info("Finished loading the client in ${System.currentTimeMillis() - lastTick}ms.")

        // Set is starting status
        isStarting = false
    }

    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        // Call client shutdown
        eventManager.callEvent(ClientShutdownEvent())

        // Save all available configs
        fileManager.saveAllConfigs()

        // Shutdown discord rpc
        clientRichPresence.shutdown()
    }

}