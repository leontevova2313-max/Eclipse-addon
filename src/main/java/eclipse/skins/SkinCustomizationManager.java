package eclipse.skins;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkinCustomizationManager {
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{32,36})\"");
    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([A-Za-z0-9_]{1,16})\"");
    private static final Pattern CAPE_PATTERN = Pattern.compile("\\{[^{}]*\"id\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"state\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"url\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"alias\"\\s*:\\s*\"([^\"]+)\"[^{}]*}");
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Identifier LOCAL_SKIN_ID = Identifier.of("eclipse", "local_skin_preview");
    private static final Identifier LOCAL_CAPE_ID = Identifier.of("eclipse", "local_cape");
    private static final Map<String, SkinTextures> OFFICIAL_SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, AssetInfo.TextureAssetInfo> OFFICIAL_CAPE_CACHE = new ConcurrentHashMap<>();
    private static final List<String> KNOWN_ACCOUNTS = new ArrayList<>();

    private static SkinSource skinSource = SkinSource.Default;
    private static SkinModel skinModel = SkinModel.Auto;
    private static CapeSource capeSource = CapeSource.Default;
    private static String officialName = "";
    private static String resolvedOfficialName = "";
    private static Path loadedSkinFile;
    private static String loadedSkinFileName = "No skin loaded";
    private static volatile AssetInfo.TextureAssetInfo localSkinAsset;
    private static volatile SkinTextures localSkinTextures;
    private static volatile PlayerSkinType localSkinType = PlayerSkinType.WIDE;
    private static Path localCapeFile;
    private static final List<CapeInfo> officialCapes = new ArrayList<>();
    private static int officialCapeIndex;
    private static String selectedOfficialCapeId = "";
    private static volatile AssetInfo.TextureAssetInfo selectedOfficialCapeAsset;
    private static volatile SkinTextures officialTextures;
    private static volatile AssetInfo.TextureAssetInfo localCapeAsset;
    private static volatile String status = "Idle";
    private static CompletableFuture<?> loadingTask;
    private static String loadingOfficialName = "";
    private static boolean loaded;

    private SkinCustomizationManager() {
    }

    public static SkinSource skinSource() {
        return skinSource;
    }

    public static void skinSource(SkinSource value) {
        skinSource = value;
        save();
        if (value == SkinSource.Official) refreshOfficialPreview();
    }

    public static SkinModel skinModel() {
        return skinModel;
    }

    public static void skinModel(SkinModel value) {
        skinModel = value;
        save();
    }

    public static CapeSource capeSource() {
        return capeSource;
    }

    public static void capeSource(CapeSource value) {
        capeSource = value;
        save();
        if (value == CapeSource.Official) refreshAccountProfile();
        if (value == CapeSource.LocalFile) loadLocalCape();
    }

    public static String officialName() {
        return officialName;
    }

    public static void officialName(String value) {
        officialName = value.trim();
        refreshKnownAccounts();
        save();
    }

    public static String activeAccountName() {
        String selected = selectedOfficialName();
        return selected.isBlank() ? "No account" : selected;
    }

    public static String accountButtonText() {
        refreshKnownAccounts();
        if (KNOWN_ACCOUNTS.isEmpty()) return "Account: unavailable";
        return "Account: " + activeAccountName() + (canManageSelectedAccount() ? " *" : " preview");
    }

    public static void nextAccount() {
        refreshKnownAccounts();
        if (KNOWN_ACCOUNTS.isEmpty()) {
            status = "No accounts available";
            return;
        }

        String current = activeAccountName();
        int index = KNOWN_ACCOUNTS.indexOf(current);
        String next = KNOWN_ACCOUNTS.get((index + 1 + KNOWN_ACCOUNTS.size()) % KNOWN_ACCOUNTS.size());
        String sessionName = sessionUsername();
        officialName = next.equalsIgnoreCase(sessionName) ? "" : next;
        skinSource = SkinSource.Official;
        capeSource = canManageSelectedAccount() ? CapeSource.Official : CapeSource.Default;
        save();
        refreshOfficialPreview();
        if (canManageSelectedAccount()) refreshAccountProfile();
        else status = "Preview only. Launch with this account to manage capes.";
    }

    public static String displayName() {
        return switch (skinSource) {
            case Official -> {
                String name = !resolvedOfficialName.isBlank() ? resolvedOfficialName : selectedOfficialName();
                yield name.isBlank() ? "Official Skin" : name;
            }
            case LocalFile -> loadedSkinFileName == null || loadedSkinFileName.isBlank() || loadedSkinFileName.equals("No skin loaded")
                ? "Local Skin"
                : loadedSkinFileName;
            case Default -> {
                MinecraftClient client = MinecraftClient.getInstance();
                yield client == null || client.getSession() == null ? "Default Skin" : client.getSession().getUsername();
            }
        };
    }

    public static String status() {
        return status;
    }

    public static String loadedSkinFileName() {
        return loadedSkinFileName;
    }

    public static boolean hasLoadedSkinFile() {
        return loadedSkinFile != null && Files.isRegularFile(loadedSkinFile) && localSkinAsset != null;
    }

    public static void loadSkinFile(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            status = "Skin file not found";
            return;
        }

        status = "Loading skin file";
        loadingTask = CompletableFuture.runAsync(() -> {
            try (InputStream stream = Files.newInputStream(file)) {
                NativeImage image = NativeImage.read(stream);
                validateSkinImage(image);

                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "Eclipse skin preview", image);
                    client.getTextureManager().destroyTexture(LOCAL_SKIN_ID);
                    client.getTextureManager().registerTexture(LOCAL_SKIN_ID, texture);
                    localSkinAsset = new AssetInfo.TextureAssetInfo(LOCAL_SKIN_ID);
                    loadedSkinFile = file;
                    loadedSkinFileName = file.getFileName().toString();
                    skinSource = SkinSource.LocalFile;
                    localSkinType = selectedPlayerSkinType();
                    localSkinTextures = new SkinTextures(localSkinAsset, null, null, localSkinType, false);
                    save();
                    status = "Loaded " + loadedSkinFileName;
                });
            } catch (IOException exception) {
                status = "Invalid PNG";
            } catch (RuntimeException exception) {
                status = exception.getMessage();
            }
        });
    }

    public static String selectedCapeName() {
        if (officialCapes.isEmpty()) return "No owned capes";
        officialCapeIndex = Math.max(0, Math.min(officialCapeIndex, officialCapes.size() - 1));
        CapeInfo cape = officialCapes.get(officialCapeIndex);
        return cape.alias + (cape.active ? " *" : "");
    }

    public static void nextOfficialCape() {
        if (!canManageSelectedAccount()) {
            status = "Cape management requires the active Minecraft session";
            return;
        }

        if (officialCapes.isEmpty()) {
            status = "No owned capes";
            return;
        }

        officialCapeIndex = (officialCapeIndex + 1) % officialCapes.size();
        selectedOfficialCapeId = officialCapes.get(officialCapeIndex).id;
        capeSource = CapeSource.Official;
        save();
        loadSelectedOfficialCapePreview();
    }

    public static void localCapeFile(Path file) {
        localCapeFile = file;
        capeSource = CapeSource.LocalFile;
        save();
        loadLocalCape();
    }

    public static void applyLoadedOfficialSkin() {
        if (!hasLoadedSkinFile()) {
            status = "Load a skin PNG first";
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSession() == null || client.getSession().getAccessToken() == null || client.getSession().getAccessToken().isBlank()) {
            status = "Official account token unavailable";
            return;
        }

        Path file = loadedSkinFile;
        status = "Uploading skin";
        loadingTask = CompletableFuture.runAsync(() -> {
            try {
                String boundary = "----EclipseSkin" + System.nanoTime();
                byte[] body = multipartSkinBody(boundary, file, skinModel.uploadVariant());
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile/skins"))
                    .header("Authorization", "Bearer " + MinecraftClient.getInstance().getSession().getAccessToken())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Skin upload returned " + response.statusCode());
                }

                status = "Skin updated";
                skinSource = SkinSource.Official;
                save();
                refreshOfficialPreview();
                refreshAccountProfile();
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                status = "Skin upload failed";
            } catch (RuntimeException exception) {
                status = exception.getMessage();
            }
        });
    }

    public static void refreshAccountProfile() {
        if (!canManageSelectedAccount()) {
            status = "Launch with this account to load official capes";
            return;
        }

        status = "Loading profile";
        loadingTask = CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = authorizedRequest("https://api.minecraftservices.com/minecraft/profile").GET().build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Profile returned " + response.statusCode());
                }

                parseCapes(response.body());
                selectSavedOrActiveCape();
                loadSelectedOfficialCapePreview();
                status = "Profile loaded";
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                status = "Profile load failed";
            } catch (RuntimeException exception) {
                status = exception.getMessage();
            }
        });
    }

    public static void applySelectedOfficialCape() {
        if (!canManageSelectedAccount()) {
            status = "Launch with this account to apply capes";
            return;
        }

        if (officialCapes.isEmpty()) {
            status = "No owned capes";
            return;
        }

        CapeInfo cape = officialCapes.get(Math.max(0, Math.min(officialCapeIndex, officialCapes.size() - 1)));
        status = "Applying cape";
        loadingTask = CompletableFuture.runAsync(() -> {
            try {
                String json = "{\"capeId\":\"" + cape.id + "\"}";
                HttpRequest request = authorizedRequest("https://api.minecraftservices.com/minecraft/profile/capes/active")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Cape returned " + response.statusCode());
                }

                parseCapes(response.body());
                selectedOfficialCapeId = cape.id;
                refreshOfficialPreview();
                loadSelectedOfficialCapePreview();
                status = "Cape applied";
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                status = "Cape apply failed";
            } catch (RuntimeException exception) {
                status = exception.getMessage();
            }
        });
    }

    public static void disableOfficialCape() {
        if (!canManageSelectedAccount()) {
            status = "Launch with this account to disable capes";
            return;
        }

        status = "Disabling cape";
        loadingTask = CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = authorizedRequest("https://api.minecraftservices.com/minecraft/profile/capes/active")
                    .DELETE()
                    .build();

                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Cape disable returned " + response.statusCode());
                }

                parseCapes(response.body());
                selectedOfficialCapeId = "";
                selectedOfficialCapeAsset = null;
                refreshOfficialPreview();
                status = "Cape disabled";
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                status = "Cape disable failed";
            } catch (RuntimeException exception) {
                status = exception.getMessage();
            }
        });
    }

    public static SkinTextures overrideSkin(GameProfile profile) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || profile == null || !isLocalProfile(profile)) return null;

        SkinTextures base = null;
        if (skinSource == SkinSource.Official) base = officialTextures;
        if (base == null && ((capeSource == CapeSource.LocalFile && localCapeAsset != null) || (capeSource == CapeSource.Official && selectedOfficialCapeAsset != null))) base = currentVanillaTextures();
        return applyCapeOverride(base);
    }

    public static SkinTextures currentSkinTextures() {
        SkinTextures base;
        if (skinSource == SkinSource.LocalFile && localSkinAsset != null) {
            if (localSkinTextures == null) localSkinTextures = new SkinTextures(localSkinAsset, null, null, localSkinType, false);
            base = localSkinTextures;
        } else {
            base = skinSource == SkinSource.Official && officialTextures != null
                ? officialTextures
                : currentVanillaTextures();
        }

        return applyCapeOverride(base);
    }

    public static Path directory() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.runDirectory.toPath().resolve("eclipse-skins");
    }

    public static void refreshOfficialPreview() {
        refreshOfficialPreview(false);
    }

    public static void forceRefreshOfficialPreview() {
        refreshOfficialPreview(true);
    }

    private static void refreshOfficialPreview(boolean force) {
        String name = selectedOfficialName();

        if (!name.matches("[A-Za-z0-9_]{1,16}")) {
            status = "Bad username";
            return;
        }

        String key = name.toLowerCase(Locale.ROOT);
        if (!force) {
            SkinTextures cached = OFFICIAL_SKIN_CACHE.get(key);
            if (cached != null) {
                officialTextures = cached;
                resolvedOfficialName = name;
                refreshKnownAccounts();
                skinSource = SkinSource.Official;
                status = "Loaded " + name;
                save();
                return;
            }

            if (loadingTask != null && !loadingTask.isDone() && key.equals(loadingOfficialName)) return;
        }

        status = "Loading " + name;
        loadingOfficialName = key;
        loadingTask = CompletableFuture
            .supplyAsync(() -> resolveProfile(name))
            .thenCompose(profile -> MinecraftClient.getInstance().getSkinProvider().fetchSkinTextures(profile))
            .thenAccept(optional -> {
                if (optional.isEmpty()) {
                    status = "No skin for " + name;
                    return;
                }

                SkinTextures textures = optional.get();
                OFFICIAL_SKIN_CACHE.put(key, textures);
                officialTextures = textures;
                resolvedOfficialName = name;
                skinSource = SkinSource.Official;
                save();
                status = "Loaded " + name;
            })
            .exceptionally(exception -> {
                status = "Failed to load " + name;
                return null;
            });
    }

    public static void load() {
        if (loaded) {
            restoreConfiguredSkin();
            return;
        }

        loaded = true;
        Path config = directory().resolve("customization.txt");
        if (!Files.exists(config)) {
            refreshAccountProfile();
            return;
        }

        try {
            for (String line : Files.readAllLines(config)) {
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;

                switch (parts[0]) {
                    case "skin-source" -> skinSource = SkinSource.from(parts[1]);
                    case "skin-model" -> skinModel = SkinModel.from(parts[1]);
                    case "cape-source" -> capeSource = CapeSource.from(parts[1]);
                    case "official-name" -> officialName = parts[1].trim();
                    case "resolved-official-name" -> resolvedOfficialName = parts[1].trim();
                    case "selected-official-cape" -> selectedOfficialCapeId = parts[1].trim();
                    case "loaded-skin" -> loadedSkinFile = parts[1].isBlank() ? null : Path.of(parts[1]);
                    case "loaded-skin-name" -> loadedSkinFileName = parts[1].isBlank() ? "No skin loaded" : parts[1];
                    case "official-cape-index" -> officialCapeIndex = parseInt(parts[1]);
                    case "local-cape" -> localCapeFile = parts[1].isBlank() ? null : Path.of(parts[1]);
                    default -> {
                    }
                }
            }
        } catch (IOException ignored) {
        }

        restoreConfiguredSkin();
    }

    public static void save() {
        Path dir = directory();
        Path config = dir.resolve("customization.txt");

        try {
            Files.createDirectories(dir);
            Files.writeString(config,
                "skin-source=" + skinSource.id + "\n"
                    + "skin-model=" + skinModel.id + "\n"
                    + "cape-source=" + capeSource.id + "\n"
                    + "official-name=" + officialName + "\n"
                    + "resolved-official-name=" + resolvedOfficialName + "\n"
                    + "selected-official-cape=" + selectedOfficialCapeId + "\n"
                    + "loaded-skin=" + (loadedSkinFile == null ? "" : loadedSkinFile) + "\n"
                    + "loaded-skin-name=" + loadedSkinFileName + "\n"
                    + "official-cape-index=" + officialCapeIndex + "\n"
                    + "local-cape=" + (localCapeFile == null ? "" : localCapeFile) + "\n");
        } catch (IOException ignored) {
        }
    }

    private static SkinTextures currentVanillaTextures() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;

        Supplier<SkinTextures> supplier = client.getSkinProvider().supplySkinTextures(client.getGameProfile(), false);
        return supplier.get();
    }

    private static void restoreConfiguredSkin() {
        refreshKnownAccounts();
        if (skinSource == SkinSource.Official) refreshOfficialPreview();
        if (skinSource == SkinSource.LocalFile && loadedSkinFile != null && localSkinAsset == null) loadSkinFile(loadedSkinFile);
        if (capeSource == CapeSource.Official) refreshAccountProfile();
        if (capeSource == CapeSource.LocalFile) loadLocalCape();
    }

    private static String selectedOfficialName() {
        if (!officialName.isBlank()) return officialName.trim();
        return sessionUsername();
    }

    private static SkinTextures applyCapeOverride(SkinTextures base) {
        if (base == null) return null;
        AssetInfo.TextureAssetInfo capeAsset = null;
        if (capeSource == CapeSource.LocalFile) capeAsset = localCapeAsset;
        if (capeSource == CapeSource.Official) capeAsset = selectedOfficialCapeAsset;
        if (capeAsset == null) return base;

        SkinTextures.SkinOverride override = SkinTextures.SkinOverride.create(
            Optional.empty(),
            Optional.of(capeAsset),
            Optional.of(capeAsset),
            Optional.empty()
        );
        return base.withOverride(override);
    }

    private static void refreshKnownAccounts() {
        KNOWN_ACCOUNTS.clear();
        addKnownAccount(sessionUsername());
        addKnownAccount(officialName);
        addKnownAccount(resolvedOfficialName);
    }

    private static void addKnownAccount(String name) {
        if (name == null || name.isBlank()) return;
        for (String known : KNOWN_ACCOUNTS) {
            if (known.equalsIgnoreCase(name)) return;
        }
        KNOWN_ACCOUNTS.add(name);
    }

    private static boolean canManageSelectedAccount() {
        String session = sessionUsername();
        String selected = selectedOfficialName();
        return !session.isBlank() && !selected.isBlank() && session.equalsIgnoreCase(selected);
    }

    private static String sessionUsername() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null || client.getSession() == null ? "" : client.getSession().getUsername();
    }

    private static void selectSavedOrActiveCape() {
        if (officialCapes.isEmpty()) {
            officialCapeIndex = 0;
            selectedOfficialCapeAsset = null;
            return;
        }

        int selectedIndex = -1;
        if (!selectedOfficialCapeId.isBlank()) {
            for (int i = 0; i < officialCapes.size(); i++) {
                if (officialCapes.get(i).id.equals(selectedOfficialCapeId)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        if (selectedIndex < 0) {
            for (int i = 0; i < officialCapes.size(); i++) {
                if (officialCapes.get(i).active) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        officialCapeIndex = selectedIndex < 0 ? 0 : selectedIndex;
        selectedOfficialCapeId = officialCapes.get(officialCapeIndex).id;
    }

    private static void loadSelectedOfficialCapePreview() {
        if (officialCapes.isEmpty()) return;
        CapeInfo cape = officialCapes.get(Math.max(0, Math.min(officialCapeIndex, officialCapes.size() - 1)));
        AssetInfo.TextureAssetInfo cached = OFFICIAL_CAPE_CACHE.get(cape.id);
        if (cached != null) {
            selectedOfficialCapeAsset = cached;
            status = "Cape preview: " + cape.alias;
            return;
        }

        status = "Loading cape preview";
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(cape.url)).GET().build();
                HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Cape returned " + response.statusCode());
                }

                NativeImage image;
                try (InputStream stream = response.body()) {
                    image = NativeImage.read(stream);
                }

                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    Identifier id = Identifier.of("eclipse", "official_cape_" + sanitizeIdentifier(cape.id));
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "Eclipse official cape", image);
                    client.getTextureManager().destroyTexture(id);
                    client.getTextureManager().registerTexture(id, texture);
                    AssetInfo.TextureAssetInfo asset = new AssetInfo.TextureAssetInfo(id);
                    OFFICIAL_CAPE_CACHE.put(cape.id, asset);
                    selectedOfficialCapeAsset = asset;
                    capeSource = CapeSource.Official;
                    save();
                    status = "Cape preview: " + cape.alias;
                });
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                status = "Cape preview failed";
            } catch (RuntimeException exception) {
                status = exception.getMessage();
            }
        });
    }

    private static String sanitizeIdentifier(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            builder.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' ? c : '_');
        }
        return builder.toString();
    }

    private static void loadLocalCape() {
        if (localCapeFile == null || !Files.isRegularFile(localCapeFile)) {
            localCapeAsset = null;
            status = "Cape file not found";
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        try (InputStream stream = Files.newInputStream(localCapeFile)) {
            NativeImage image = NativeImage.read(stream);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "Eclipse local cape", image);
            client.getTextureManager().destroyTexture(LOCAL_CAPE_ID);
            client.getTextureManager().registerTexture(LOCAL_CAPE_ID, texture);
            localCapeAsset = new AssetInfo.TextureAssetInfo(LOCAL_CAPE_ID);
            status = "Local cape loaded";
        } catch (IOException exception) {
            localCapeAsset = null;
            status = "Cape load failed";
        }
    }

    private static void validateSkinImage(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean classicSkin = width == 64 && (height == 64 || height == 32);
        boolean highResolutionSkin = width == 128 && height == 128;
        if (!classicSkin && !highResolutionSkin) {
            throw new IllegalArgumentException("Skin PNG must be 64x64, 64x32, or 128x128");
        }
    }

    private static PlayerSkinType selectedPlayerSkinType() {
        return skinModel == SkinModel.Slim ? PlayerSkinType.SLIM : PlayerSkinType.WIDE;
    }

    private static void parseCapes(String body) {
        List<CapeInfo> capes = new ArrayList<>();
        Matcher matcher = CAPE_PATTERN.matcher(body);
        while (matcher.find()) {
            capes.add(new CapeInfo(
                matcher.group(1),
                unescape(matcher.group(4)),
                matcher.group(3),
                matcher.group(2).equalsIgnoreCase("ACTIVE")
            ));
        }

        officialCapes.clear();
        officialCapes.addAll(capes);
        if (officialCapeIndex >= officialCapes.size()) officialCapeIndex = Math.max(0, officialCapes.size() - 1);
    }

    private static HttpRequest.Builder authorizedRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", "Bearer " + MinecraftClient.getInstance().getSession().getAccessToken());
    }

    private static GameProfile resolveProfile(String name) {
        try {
            URI uri = URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name);
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("Lookup returned " + response.statusCode());

            String body = response.body();
            Matcher idMatcher = ID_PATTERN.matcher(body);
            Matcher nameMatcher = NAME_PATTERN.matcher(body);
            if (!idMatcher.find()) throw new IllegalStateException("Missing UUID");

            UUID uuid = parseUuid(idMatcher.group(1));
            String resolvedName = nameMatcher.find() ? nameMatcher.group(1) : name;
            return new GameProfile(uuid, resolvedName);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static byte[] multipartSkinBody(String boundary, Path file, String variant) throws IOException {
        String fileName = file.getFileName().toString().replace("\"", "");
        String start = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"variant\"\r\n\r\n"
            + variant + "\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
            + "Content-Type: image/png\r\n\r\n";
        String end = "\r\n--" + boundary + "--\r\n";

        byte[] fileBytes = Files.readAllBytes(file);
        byte[] startBytes = start.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] endBytes = end.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] body = new byte[startBytes.length + fileBytes.length + endBytes.length];
        System.arraycopy(startBytes, 0, body, 0, startBytes.length);
        System.arraycopy(fileBytes, 0, body, startBytes.length, fileBytes.length);
        System.arraycopy(endBytes, 0, body, startBytes.length + fileBytes.length, endBytes.length);
        return body;
    }

    private static boolean isLocalProfile(GameProfile profile) {
        MinecraftClient client = MinecraftClient.getInstance();
        GameProfile local = client.getGameProfile();
        UUID uuid = profile.id();
        UUID localUuid = local.id();
        if (uuid != null && localUuid != null) return uuid.equals(localUuid);
        return profile.name() != null && profile.name().equalsIgnoreCase(local.name());
    }

    private static UUID parseUuid(String value) {
        if (value.contains("-")) return UUID.fromString(value);
        String lower = value.toLowerCase(Locale.ROOT);
        return UUID.fromString(lower.substring(0, 8) + "-"
            + lower.substring(8, 12) + "-"
            + lower.substring(12, 16) + "-"
            + lower.substring(16, 20) + "-"
            + lower.substring(20));
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\/", "/");
    }

    public enum SkinSource {
        Default("default"),
        LocalFile("local-file"),
        Official("official");

        private final String id;

        SkinSource(String id) {
            this.id = id;
        }

        public SkinSource next() {
            return switch (this) {
                case Default -> LocalFile;
                case LocalFile -> Official;
                case Official -> Default;
            };
        }

        private static SkinSource from(String id) {
            for (SkinSource source : values()) {
                if (source.id.equalsIgnoreCase(id)) return source;
            }

            return Default;
        }
    }

    public enum SkinModel {
        Auto("auto"),
        Classic("classic"),
        Slim("slim");

        private final String id;

        SkinModel(String id) {
            this.id = id;
        }

        public SkinModel next() {
            return switch (this) {
                case Auto -> Classic;
                case Classic -> Slim;
                case Slim -> Auto;
            };
        }

        private String uploadVariant() {
            return this == Slim ? Slim.id : Classic.id;
        }

        private static SkinModel from(String id) {
            for (SkinModel model : values()) {
                if (model.id.equalsIgnoreCase(id)) return model;
            }

            return Auto;
        }
    }

    public enum CapeSource {
        Default("default"),
        LocalFile("local-file"),
        Official("official");

        private final String id;

        CapeSource(String id) {
            this.id = id;
        }

        public CapeSource next() {
            return switch (this) {
                case Default -> LocalFile;
                case LocalFile -> Official;
                case Official -> Default;
            };
        }

        private static CapeSource from(String id) {
            for (CapeSource source : values()) {
                if (source.id.equalsIgnoreCase(id)) return source;
            }

            return Default;
        }
    }

    private record CapeInfo(String id, String alias, String url, boolean active) {
    }
}
