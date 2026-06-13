package dev.httxrafa.modflared.binary.download;

import dev.httxrafa.modflared.tunnel.RunningTunnel;

/**
 * Describes an OS/arch-specific cloudflared binary distribution.
 */
public class CloudflaredDownload {

    private final String osName;
    private final String downloadFile;
    private final String fileName;

    public CloudflaredDownload(String osName, String downloadFile, String fileName) {
        this.osName = osName;
        this.downloadFile = downloadFile;
        this.fileName = fileName;
    }

    public String osName() {
        return osName;
    }

    public String downloadFile() {
        return downloadFile;
    }

    public String fileName() {
        return fileName;
    }

    /**
     * Detects the current OS and architecture and returns the appropriate download descriptor.
     */
    public static CloudflaredDownload find() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (RunningTunnel.isWindows()) {
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                return new CloudflaredDownload(os, "cloudflared-windows-arm64.exe", "cloudflared.exe");
            }
            return new CloudflaredDownload(os, "cloudflared-windows-amd64.exe", "cloudflared.exe");
        } else if (os.contains("mac os x") || os.contains("darwin")) {
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                return new CloudflaredDownload(os, "cloudflared-darwin-arm64.tgz", "cloudflared");
            }
            return new CloudflaredDownload(os, "cloudflared-darwin-amd64.tgz", "cloudflared");
        } else {
            // Linux
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                return new CloudflaredDownload(os, "cloudflared-linux-arm64", "cloudflared");
            } else if (arch.contains("arm")) {
                return new CloudflaredDownload(os, "cloudflared-linux-arm", "cloudflared");
            }
            return new CloudflaredDownload(os, "cloudflared-linux-amd64", "cloudflared");
        }
    }
}
