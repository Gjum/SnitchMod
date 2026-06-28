package gjum.minecraft.civ.snitchmod.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JalistAutoPaginator {
    private static final Minecraft mc = Minecraft.getInstance();
    private static JalistAutoPaginator instance;
    
    private boolean isActive = false;
    private boolean waitingForNextPage = false;
    private int pagesProcessed = 0;
    private int totalSnitchesFound = 0;
    private int snitchesWillCullSoon = 0; // Count snitches that will cull in next 48h
    private int snitchesWillDormantSoon = 0; // Count snitches that will go dormant in next 48h
    private Set<String> groupsFound = new HashSet<>();
    private long startTime = 0;
    
    // JAList GUI detection
    private static final String JALIST_TITLE = "JukeAlert snitches";
    private static final int NEXT_PAGE_SLOT = 53; // Bottom right slot in a 54-slot inventory
    
    public static JalistAutoPaginator getInstance() {
        if (instance == null) {
            instance = new JalistAutoPaginator();
        }
        return instance;
    }
    
    public void startAutoPagination() {
        if (isActive) {
            logToChat("Auto-pagination already running!");
            return;
        }
        
        if (!isJalistOpen()) {
            logToChat("JAList must be open to start auto-pagination! Press J key while in JAList.");
            return;
        }
        
        isActive = true;
        waitingForNextPage = false;
        pagesProcessed = 0;
        totalSnitchesFound = 0;
        snitchesWillCullSoon = 0;
        snitchesWillDormantSoon = 0;
        groupsFound.clear();
        startTime = System.currentTimeMillis();
        
        logToChat("Starting JAList auto-pagination... This will read all pages automatically.");
        
        // Start the first page click immediately
        mc.execute(() -> clickNextPage(((AbstractContainerScreen<?>) mc.screen).getMenu().getItems()));
    }
    
    public void stopAutoPagination() {
        if (!isActive) return;
        
        isActive = false;
        waitingForNextPage = false;
        
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        logToChat(String.format(
            "JAList scan complete! Found %d snitches across %d groups in %d pages (%ds)",
            totalSnitchesFound, groupsFound.size(), pagesProcessed, duration
        ));
        
        // Add warnings about snitches that will cull or go dormant soon
        if (snitchesWillCullSoon > 0 || snitchesWillDormantSoon > 0) {
            StringBuilder warning = new StringBuilder("⚠ Warning: You have ");
            
            if (snitchesWillCullSoon > 0 && snitchesWillDormantSoon > 0) {
                warning.append(String.format("%d snitches that will be culled and %d that will go dormant in the next 48h!", snitchesWillCullSoon, snitchesWillDormantSoon));
            } else if (snitchesWillCullSoon > 0) {
                warning.append(String.format("%d snitches that will be culled in the next 48h!", snitchesWillCullSoon));
            } else {
                warning.append(String.format("%d snitches that will go dormant in the next 48h!", snitchesWillDormantSoon));
            }
            
            warning.append(" Use https://civinfo.net/snitches/map to see where they are!");
            logToChat(warning.toString());
        }
    }
    
    public void onJalistPageLoaded(List<ItemStack> stacks, int snitchCount) {
        if (!isActive) return;
        
        pagesProcessed++;
        totalSnitchesFound += snitchCount;
        waitingForNextPage = false;
        
        // Show progress every 10 pages instead of 5
        if (pagesProcessed % 10 == 0) {
            logToChat(String.format("Progress: %d pages (%d snitches so far)", pagesProcessed, totalSnitchesFound));
        }
        
        // Click next page instantly on page load for maximum speed
        mc.execute(() -> clickNextPage(stacks));
    }
    
    public void addSnitchEntry(String group, long dormantTs, long cullTs) {
        if (group != null && !group.trim().isEmpty()) {
            groupsFound.add(group);
        }
        
        long currentTime = System.currentTimeMillis();
        long fortyEightHoursFromNow = currentTime + (48L * 60L * 60L * 1000L); // 48 hours in milliseconds
        
        if (cullTs != 0 && cullTs <= fortyEightHoursFromNow) {
            snitchesWillCullSoon++;
        }
        
        if (dormantTs != 0 && dormantTs <= fortyEightHoursFromNow) {
            snitchesWillDormantSoon++;
        }
    }
    
    private void clickNextPage(List<ItemStack> stacks) {
        if (!isActive || waitingForNextPage) {
            return;
        }
        
        var screen = mc.screen;
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            stopAutoPagination();
            return;
        }
        
        if (!isJalistOpen()) {
            stopAutoPagination();
            return;
        }
        
        // Get the next page button (arrow in slot 53)
        var container = containerScreen.getMenu();
        if (stacks.size() <= NEXT_PAGE_SLOT) {
            stopAutoPagination();
            return;
        }

        ItemStack nextPageItem = stacks.get(NEXT_PAGE_SLOT);
        
        // Check if there's a next page (should be an arrow item)
        if (nextPageItem.isEmpty() || !isArrowItem(nextPageItem)) {
            stopAutoPagination();
            return;
        }
        
        // Click the next page button
        waitingForNextPage = true;
        
        // Simulate a left click on the slot
        if (mc.gameMode != null) {
            mc.gameMode.handleContainerInput(
                container.containerId,
                NEXT_PAGE_SLOT,
                0,
                ContainerInput.PICKUP,
                mc.player
            );
        }
    }
    
    private boolean isJalistOpen() {
        var screen = mc.screen;
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        
        var title = containerScreen.getTitle().getString();
        return title.contains("JukeAlert") || title.contains("snitches") || title.toLowerCase().contains("your snitches");
    }
    
    private boolean isArrowItem(ItemStack item) {
        // Check for common arrow items used for pagination
        return item.is(Items.ARROW) || 
               item.is(Items.SPECTRAL_ARROW) ||
               item.getDisplayName().getString().toLowerCase().contains("next");
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public boolean isWaitingForNextPage() {
        return waitingForNextPage;
    }
    
    public static void onTick() {
        // No longer needed for this approach
    }
    
    public static void onChatMessage(String message) {
        // No longer needed for this approach  
    }
    
    public static boolean isRunning() {
        return getInstance().isActive();
    }
    
    public static int getCurrentPage() {
        return getInstance().pagesProcessed;
    }
    
    private void logToChat(String message) {
        mc.gui.getChat().addClientSystemMessage(Component.literal("[JAList Auto] " + message));
    }
}