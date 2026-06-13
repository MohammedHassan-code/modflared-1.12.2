# Modflared (Forge 1.12.2 Port)

<div align="center">
  <img src="src/main/resources/modflared.png" width="128" height="128" alt="Modflared Logo">
  <br>
  <p><b>Automatically connect to Cloudflare tunnels in Minecraft Forge 1.12.2</b></p>
</div>

---

## 🌟 Overview
**Modflared** is a client-side mod that automatically downloads and manages the `cloudflared` binary, allowing players to connect to your Minecraft server securely via Cloudflare Tunnels without needing to configure or install anything manually.

This is a port of the original Modflared project, fully backported to the classic Minecraft Forge 1.12.2.

> ✨ *Note: This port is fully **vibe coded**, with an emphasis on seamless UX, custom icon badges, and flawless backend tunneling.*

### Credits
* **Original Creator:** [HttpRafa](https://github.com/HttpRafa)

---

## 🚀 Features
* **Zero Configuration for Players:** Players just type the domain name; the mod handles the rest invisibly.
* **Auto-Detection (DNS TXT):** If your domain has the correct DNS records, the mod kicks in automatically!
* **Server List UI Integration:** Custom Cloudflare indicator badges dynamically show when a connection is safely tunneled.
* **Invisible Background Daemon:** Downloads and runs the Cloudflare tunnel client in the background exclusively when needed.

---

## 🛠️ For Server Owners: Setup Guide

To let your players connect automatically without forcing them to configure local IPs:

1. **Host your Server via Cloudflare Tunnel.**
2. Set up a **DNS TXT Record** on the domain you use for your server:
   - **Type:** `TXT`
   - **Name:** Your subdomain (e.g. `play.yourdomain.com`)
   - **Value:** `cloudflared-use-tunnel`
3. Tell your players to download **Modflared** and connect to your domain normally. That's it!

*(Alternatively, players can manually add IPs to `.minecraft/modflared/forced_tunnels.json` if you do not own the domain).*

---

## 💻 Building from Source

This project uses ForgeGradle.

1. Clone the repository:
   ```bash
   git clone https://github.com/YourName/modflared-1.12.2.git
   cd modflared-1.12.2
   ```

2. Build the JAR:
   ```bash
   ./gradlew build
   ```

3. Find the compiled output in `build/libs/`.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
