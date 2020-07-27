package not.hub.packetderp;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MojangApi {

    private static final LoadingCache<String, Profile> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build(new CacheLoader<String, Profile>() {
                private final HttpProfileRepository repo = new HttpProfileRepository("minecraft");

                @Override
                public Profile load(@Nonnull String name) {
                    return repo.findProfilesByNames(name)[0];
                }
            });

    public static UUID getUuidByName(String name) throws ExecutionException {
        return UUID.fromString(cache.get(name).getId().replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
    }

}
