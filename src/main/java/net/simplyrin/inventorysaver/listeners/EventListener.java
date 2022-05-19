package net.simplyrin.inventorysaver.listeners;

import java.io.File;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import lombok.AllArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.simplyrin.inventorysaver.InventorySaver;
import net.simplyrin.inventorysaver.utils.InventoryData;

/**
 * Created by SimplyRin on 2019/12/16.
 *
 * Copyright (c) 2019-2022 SimplyRin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@AllArgsConstructor
public class EventListener implements Listener {

	private InventorySaver instance;
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		this.instance.getPlayerConfig().set("name." + player.getName().toLowerCase(), player.getUniqueId().toString());
		this.instance.getPlayerConfig().set("uuid." + player.getUniqueId().toString(), player.getName());
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getSlot() == -999) {
			return;
		}

		Player player = (Player) event.getWhoClicked();

		Inventory inventory = event.getInventory();
		InventoryView inventoryView = event.getView();

		String title = inventoryView.getTitle();
		int slot = event.getSlot();
		
		if (title != null && title.equals("Temporary inventory") && (slot == 43 || slot == 44)) {
			if (slot == 43) {
				event.setCancelled(true);
				return;
			}
			
			ItemStack itemStack = inventory.getItem(event.getSlot());
			if (itemStack.hasItemMeta() && ChatColor.stripColor(itemStack.getItemMeta().getDisplayName()).equals("<- Back")) {
				this.instance.getViewInventory().openPreviousInventory(player);
				return;
			}
		}

		if (title != null && title.startsWith("Directory:")) {
			event.setCancelled(true);

			String page = title.split(" ")[2];
			page = page.replace("(", "");
			page = page.replace(")", "");

			String[] split = page.split("[/]");

			File directory = new File(this.instance.getDataFolder(), title.split(" ")[1]);
			
			String directoryName = title.split(" ")[1];
			String cacheId = title.split(" ")[title.split(" ").length - 1];

			ItemStack itemStack = inventory.getItem(event.getSlot());
			
			if (itemStack == null) {
				return;
			}

			if (itemStack.hasItemMeta() 
					&& itemStack.getItemMeta().getDisplayName() != null 
					&& ChatColor.stripColor(itemStack.getItemMeta().getDisplayName()).endsWith(".yml")) {
				File file = new File(directory, ChatColor.stripColor(itemStack.getItemMeta().getDisplayName()));

				InventoryData inventoryData = this.instance.getInventoryManager().getInventoryData(player, directory.getName(), file.getName());

				if (event.getClick().isRightClick()) {
					String command = "/loadinventory <player> " + title.split(" ")[1] + "/" + ChatColor.stripColor(itemStack.getItemMeta().getDisplayName());

					TextComponent textComponent = new TextComponent(ChatColor.AQUA + command);
					textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
			        player.spigot().sendMessage(textComponent);
				}

				this.instance.getViewInventory().openInventory(player, inventoryData, "Temporary inventory", inventory);
				return;
			}

			if (itemStack.hasItemMeta() && ChatColor.stripColor(itemStack.getItemMeta().getDisplayName()).equals("<- Previous Page")) {
				int i = Integer.valueOf(split[0]) - 2;
				
				if (event.isShiftClick()) {
					i -= 9;
				}

				this.instance.getViewInventory().openDirectory(player, directoryName, null, cacheId, i);
				return;
			}

			if (itemStack.hasItemMeta() && ChatColor.stripColor(itemStack.getItemMeta().getDisplayName()).equals("Next Page ->")) {
				int i = Integer.valueOf(split[0]);
				
				if (event.isShiftClick()) {
					i += 9;
				}

				this.instance.getViewInventory().openDirectory(player, directoryName, null, cacheId, i);
				return;
			}
		}
	}

}
