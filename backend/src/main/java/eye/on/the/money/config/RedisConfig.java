package eye.on.the.money.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.boot.data.redis.autoconfigure.ClientResourcesBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean
    public ClientResourcesBuilderCustomizer redisAddressResolverCustomizer() {
        return (builder) -> builder.addressResolverGroup(DefaultAddressResolverGroup.INSTANCE);
    }
}
