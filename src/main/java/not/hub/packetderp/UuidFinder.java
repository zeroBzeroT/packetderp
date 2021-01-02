package not.hub.packetderp;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.bukkit.Bukkit.getServer;

public class UuidFinder {

    private static final UUID UUID_EMPTY = new UUID(0L, 0L);

    private static final Profile PROFILE_EMPTY = new Profile() {{
        setId("00000000000000000000000000000000");
        setName("PLAYER NOT FOUND");
    }};

    private static final String REGEX_UUID_NO_DASH = "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})";
    private static final String FORMAT_UUID_DASH = "$1-$2-$3-$4-$5";


    private static final LoadingCache<String, Profile> cache = CacheBuilder
            .newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build(new CacheLoader<String, Profile>() {
                private final HttpProfileRepository repo = new HttpProfileRepository("minecraft");

                @Override
                public Profile load(@Nonnull String name) {
                    Profile[] profiles = repo.findProfilesByNames(name);
                    if (profiles.length == 0) {
                        return PROFILE_EMPTY;
                    }
                    return repo.findProfilesByNames(name)[0];
                }
            });

    public static UUID getByName(final String name) throws ExecutionException, PlayerNotFoundException {

        final String nameLowerCase = name.toLowerCase();

        final UUID uuid = getServer()
                .getOnlinePlayers()
                .stream()
                .filter(player -> player.getName().toLowerCase().equals(nameLowerCase))
                .findFirst()
                .map(Entity::getUniqueId)
                .orElse(Arrays
                        .stream(getServer().getOfflinePlayers())
                        .filter(player -> player.getName().toLowerCase().equals(nameLowerCase))
                        .findFirst()
                        .map(OfflinePlayer::getUniqueId)
                        .orElse(UUID
                                .fromString(cache
                                        .get(nameLowerCase)
                                        .getId()
                                        .replaceFirst(REGEX_UUID_NO_DASH, FORMAT_UUID_DASH))
                        )
                );

        if (uuid.equals(UUID_EMPTY)) {
            throw new PlayerNotFoundException("No UUID result via Mojang API for name: " + nameLowerCase);
        }

        return uuid;

    }

    static class PlayerNotFoundException extends Exception {
        public PlayerNotFoundException(String message) {
            super(message);
        }
    }

}
