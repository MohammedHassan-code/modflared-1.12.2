# Modflared (FORGE 1.12.2)
> ⚠️ **UNOFFICIAL FORGE BACKPORT:** This is an unofficial port of [HttpRafa's Modflared](https://github.com/HttpRafa/modflared). This is not the official repository and is not maintained by the original creator.

> 🛑 **WARNING - FORGE ONLY:** This mod is **strictly** for Minecraft Forge 1.12.2. Do **NOT** download this if you are using Fabric, NeoForge, Quilt, or any other modern mod loader. It will not work!

Automatically connects you to a [Cloudflare tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/) without having to install [cloudflared](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/) separately.

*Note: This is a backport of [HttpRafa's Modflared](https://github.com/HttpRafa/modflared) built exclusively for Minecraft Forge 1.12.2.*

## How to use
To be able to use the mod you have to be on the operating system Windows, Linux, or MacOS. Players simply need to install this mod in their `mods` folder and connect to your configured server domain.

## Other resources
For more detailed instructions, you can read [Adalie's blog post](https://dacubeking.com/2024/02/28/Proxying-Minecraft.html).

## Configuring Cloudflared
You need to set up cloudflare on your server for this to work. There's plenty of guides on how to do this elsewhere.  
Make sure in your config file (possibly in `/etc/cloudflared/config.yml`) you have the lines:
```YML
- hostname: example.domain.net
  service: tcp://localhost:25565
```
Replace `example.domain.net` with the correct subdomain you want to use. If you're running multiple instances (eg. with docker), change the port 25565 to whatever port you're using.  
Restart the cloudflare daemon (`sudo systemctl restart cloudflared`) to apply the changes.

Add the correct DNS entry: go to [Cloudflare dashboard](https://dash.cloudflare.net) and add a `TXT` record on your domain:
- **Name:** Your subdomain (e.g. `example.domain.net`)
- **Content:** `cloudflared-use-tunnel`

This TXT record is what tells the client-side mod to automatically engage the Cloudflare Tunnel!
