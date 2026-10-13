package me.copa.pstaff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PStaff extends JavaPlugin implements Listener {
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();
    private final Set<UUID> god = ConcurrentHashMap.newKeySet();
    private final Set<UUID> staffMode = ConcurrentHashMap.newKeySet();
    private final Set<UUID> muted = ConcurrentHashMap.newKeySet();
    private final Map<UUID, SavedInventory> saved = new HashMap<>();
    private final Map<UUID, Long> muteExpiry = new ConcurrentHashMap<>();
    private final Set<UUID> readonlyViews = ConcurrentHashMap.newKeySet();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    @Override public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("pStaff ativado com sucesso!");
    }

    @Override public void onDisable() {
        for (UUID id : new HashSet<>(staffMode)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) restore(p);
        }
        for (UUID id : vanished) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) for (Player viewer : Bukkit.getOnlinePlayers()) viewer.showPlayer(this, p);
        }
    }

    private String msg(String key) {
        String raw = getConfig().getString("messages." + key, "&cMensagem não configurada: " + key);
        return getConfig().getString("prefix", "&8[&bpStaff&8] &7") + raw;
    }
    private void send(CommandSender sender, String key) { sender.sendMessage(LEGACY.deserialize(msg(key))); }
    private void sendTemplate(CommandSender sender, String key, String playerName) { String raw=getConfig().getString("messages."+key, "").replace("%player%", playerName); sender.sendMessage(LEGACY.deserialize(getConfig().getString("prefix", "&8[&bpStaff&8] &7")+raw)); }
    private void send(CommandSender sender, String raw, boolean prefix) {
        String value = (prefix ? getConfig().getString("prefix", "&8[&bpStaff&8] &7") : "") + raw;
        sender.sendMessage(LEGACY.deserialize(value));
    }
    private boolean allowed(CommandSender s, String permission) {
        if (s.hasPermission("pstaff.admin") || s.hasPermission(permission)) return true;
        send(s, "no-permission"); return false;
    }
    private Player target(CommandSender sender, String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) send(sender, "player-not-found");
        return p;
    }
    private Player self(CommandSender sender) {
        if (sender instanceof Player p) return p;
        send(sender, "player-only"); return null;
    }
    private String join(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }
    private void toggleVanish(Player p) {
        if (vanished.remove(p.getUniqueId())) {
            for (Player viewer : Bukkit.getOnlinePlayers()) viewer.showPlayer(this, p);
            send(p, "vanish-disabled");
        } else {
            vanished.add(p.getUniqueId());
            for (Player viewer : Bukkit.getOnlinePlayers()) if (!viewer.hasPermission("pstaff.staff") && !viewer.hasPermission("pstaff.admin")) viewer.hidePlayer(this, p);
            send(p, "vanish-enabled");
        }
    }
    private void toggleStaff(Player p) {
        if (staffMode.contains(p.getUniqueId())) { restore(p); send(p, "staff-disabled"); return; }
        PlayerInventory inv = p.getInventory();
        saved.put(p.getUniqueId(), new SavedInventory(inv.getStorageContents(), inv.getArmorContents(), inv.getExtraContents(), p.getGameMode(), p.getAllowFlight(), p.isFlying()));
        inv.clear(); inv.setArmorContents(new ItemStack[4]); inv.setExtraContents(new ItemStack[inv.getExtraContents().length]);
        p.setGameMode(GameMode.CREATIVE);
        p.setAllowFlight(true); p.setFlying(true);
        staffMode.add(p.getUniqueId()); send(p, "staff-enabled");
    }
    private void restore(Player p) {
        SavedInventory data = saved.remove(p.getUniqueId());
        if (data != null) {
            PlayerInventory inv = p.getInventory(); inv.clear(); inv.setStorageContents(data.storage); inv.setArmorContents(data.armor); inv.setExtraContents(data.extra);
            p.setGameMode(data.mode); p.setAllowFlight(data.allowFlight); if (data.allowFlight) p.setFlying(data.flying);
        }
        staffMode.remove(p.getUniqueId());
    }
    private void openCopy(Player viewer, Player target, boolean ender) {
        Inventory gui = Bukkit.createInventory(null, 54, LEGACY.deserialize(ender ? "&5Ender Chest: " + target.getName() : "&bInventário: " + target.getName()));
        ItemStack[] contents = ender ? target.getEnderChest().getContents() : target.getInventory().getStorageContents();
        for (int i=0;i<Math.min(contents.length, 54);i++) if (contents[i] != null) gui.setItem(i, contents[i].clone());
        if (!ender) {
            ItemStack[] armor = target.getInventory().getArmorContents();
            for (int i=0;i<Math.min(armor.length,4);i++) if (armor[i] != null) gui.setItem(45+i, armor[i].clone());
        }
        readonlyViews.add(viewer.getUniqueId()); viewer.openInventory(gui);
    }

    @Override public boolean onCommand(CommandSender s, Command command, String label, String[] a) {
        String c = command.getName().toLowerCase(Locale.ROOT);
        switch (c) {
            case "staff" -> {
                if (a.length > 0 && a[0].equalsIgnoreCase("reload")) { if (!allowed(s,"pstaff.admin")) return true; reloadConfig(); send(s,"&aConfiguração recarregada.",true); return true; }
                if (a.length > 0 && a[0].equalsIgnoreCase("list")) { if (!allowed(s,"pstaff.staff")) return true; List<String> names=new ArrayList<>(); for(Player p:Bukkit.getOnlinePlayers()) if(p.hasPermission("pstaff.staff")||p.hasPermission("pstaff.admin")) names.add(p.getName()); send(s,"&bStaff online: &f"+(names.isEmpty()?"nenhum":String.join(", ",names)),true); return true; }
                if (!allowed(s,"pstaff.staff")) return true; Player p=self(s); if(p!=null) toggleStaff(p); return true;
            }
            case "vanish" -> { if(!allowed(s,"pstaff.vanish")) return true; Player p=self(s); if(p!=null) toggleVanish(p); return true; }
            case "staffchat" -> { if(!allowed(s,"pstaff.staff")) return true; if(a.length==0){send(s,"&cUse: /staffchat <mensagem>",true);return true;} String text="&8[&bSTAFF&8] &f"+s.getName()+"&7: &f"+join(a,0); for(Player p:Bukkit.getOnlinePlayers()) if(p.hasPermission("pstaff.staff")||p.hasPermission("pstaff.admin")) send(p,text,true); if(!(s instanceof Player)) send(s,text,true); return true; }
            case "invsee", "endersee" -> { String perm=c.equals("invsee")?"pstaff.invsee":"pstaff.endersee"; if(!allowed(s,perm))return true; if(a.length!=1){send(s,"&cUse: /"+c+" <jogador>",true);return true;} Player viewer=self(s), t=target(s,a[0]); if(viewer!=null&&t!=null)openCopy(viewer,t,c.equals("endersee")); return true; }
            case "freeze", "unfreeze" -> { if(!allowed(s,"pstaff.freeze"))return true; if(a.length!=1){send(s,"&cUse: /"+c+" <jogador>",true);return true;} Player t=target(s,a[0]); if(t!=null){ if(c.equals("freeze")){frozen.add(t.getUniqueId());send(t,"frozen");sendTemplate(s,"freeze-notice",t.getName());}else{frozen.remove(t.getUniqueId());send(t,"unfrozen");sendTemplate(s,"unfreeze-notice",t.getName());}} return true; }
            case "mute" -> { if(!allowed(s,"pstaff.mute"))return true; if(a.length<2){send(s,"&cUse: /mute <jogador> <tempo>",true);return true;} Player t=target(s,a[0]); if(t!=null){long duration=parseDuration(a[1]);if(duration<=0){send(s,"&cTempo inválido. Exemplos: 10m, 2h, 1d.",true);return true;}muted.add(t.getUniqueId());muteExpiry.put(t.getUniqueId(),System.currentTimeMillis()+duration);send(t,"&cVocê foi mutado temporariamente.");send(s,"&a"+t.getName()+" foi mutado.",true);}return true; }
            case "unmute" -> {if(!allowed(s,"pstaff.mute"))return true;if(a.length!=1){send(s,"&cUse: /unmute <jogador>",true);return true;}Player t=target(s,a[0]);if(t!=null){muted.remove(t.getUniqueId());muteExpiry.remove(t.getUniqueId());send(t,"&aSeu mute foi removido.");send(s,"&aMute removido de "+t.getName()+".",true);}return true;}
            case "warn", "kick" -> { String perm=c.equals("warn")?"pstaff.warn":"pstaff.kick"; if(!allowed(s,perm))return true; if(a.length<2){send(s,"&cUse: /"+c+" <jogador> <motivo>",true);return true;} Player t=target(s,a[0]); if(t!=null){String reason=join(a,1);if(c.equals("kick")){t.kick(LEGACY.deserialize("&cVocê foi expulso.\n&7Motivo: &f"+reason));send(s,"&a"+t.getName()+" foi expulso. Motivo: "+reason,true);}else{send(t,"&cVocê recebeu um aviso: &f"+reason);send(s,"&aAviso enviado para "+t.getName()+".",true);}}return true; }
            case "heal", "feed", "fly", "god" -> { String perm="pstaff."+c; if(!allowed(s,perm))return true; Player t; if(a.length>0)t=target(s,a[0]);else t=self(s); if(t==null)return true; switch(c){case "heal"->{t.setHealth(t.getMaxHealth());t.setFireTicks(0);}case "feed"->{t.setFoodLevel(20);t.setSaturation(20);}case "fly"->{t.setAllowFlight(!t.getAllowFlight());if(!t.getAllowFlight())t.setFlying(false);send(t,"&aVoo: "+(t.getAllowFlight()?"ativado":"desativado"),true);}case "god"->{if(!god.add(t.getUniqueId()))god.remove(t.getUniqueId());send(t,"&aGod: "+(god.contains(t.getUniqueId())?"ativado":"desativado"),true);}} send(s,"&aComando aplicado em "+t.getName()+".",true);return true; }
            case "clearchat" -> {if(!allowed(s,"pstaff.clearchat"))return true;for(Player p:Bukkit.getOnlinePlayers())for(int i=0;i<100;i++)p.sendMessage(" ");send(s,"&aChat limpo.",true);return true;}
            case "workbench" -> {if(!allowed(s,"pstaff.utility"))return true;Player p=self(s);if(p!=null)p.openWorkbench(null,true);return true;}
            case "trash" -> {if(!allowed(s,"pstaff.utility"))return true;Player p=self(s);if(p!=null)p.openInventory(Bukkit.createInventory(null,54,Component.text("Lixeira")));return true;}
        }
        return false;
    }

    @EventHandler public void onMove(PlayerMoveEvent e){if(!frozen.contains(e.getPlayer().getUniqueId())||e.getTo()==null)return; if(e.getFrom().getBlockX()!=e.getTo().getBlockX()||e.getFrom().getBlockY()!=e.getTo().getBlockY()||e.getFrom().getBlockZ()!=e.getTo().getBlockZ())e.setTo(e.getFrom());}
    @EventHandler public void onQuit(PlayerQuitEvent e){Player p=e.getPlayer();UUID id=p.getUniqueId();if(staffMode.contains(id))restore(p);frozen.remove(id);vanished.remove(id);god.remove(id);muted.remove(id);muteExpiry.remove(id);readonlyViews.remove(id);}
    @EventHandler public void onClick(InventoryClickEvent e){if(readonlyViews.contains(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler public void onClose(InventoryCloseEvent e){readonlyViews.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void onDamage(EntityDamageEvent e){if(e.getEntity() instanceof Player p && god.contains(p.getUniqueId()))e.setCancelled(true);}
    @EventHandler public void onChat(AsyncPlayerChatEvent e){if(muted.contains(e.getPlayer().getUniqueId())){Long until=muteExpiry.get(e.getPlayer().getUniqueId());if(until==null||until>System.currentTimeMillis()){e.setCancelled(true);e.getPlayer().sendMessage(LEGACY.deserialize("&cVocê está mutado."));}else{muted.remove(e.getPlayer().getUniqueId());muteExpiry.remove(e.getPlayer().getUniqueId());}}}

    private long parseDuration(String input){try{char unit=Character.toLowerCase(input.charAt(input.length()-1));long n=Long.parseLong(input.substring(0,input.length()-1));return n*(switch(unit){case 's'->1000L;case 'm'->60000L;case 'h'->3600000L;case 'd'->86400000L;default->0L;});}catch(Exception ex){return -1L;}}

    private record SavedInventory(ItemStack[] storage, ItemStack[] armor, ItemStack[] extra, GameMode mode, boolean allowFlight, boolean flying) {}
}
