package com.example.mall.config;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@ConfigurationProperties(prefix = "mall")
@Validated
public class MallProperties {
    @NotBlank
    private final String name;
    private final Order order;


    public MallProperties(String name, Order order) {
        this.name = name;
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public Order getOrder() {
        return order;
    }

    public static class Order {
        private final Duration payTimeout;
        @Min(value = 1, message = "单笔至少 1 件")
        @Max(value = 100, message = "单笔最多 100 件")
        private final int maxItems;

        public Order(Duration payTimeout, int maxItems) {
            this.payTimeout = payTimeout;
            this.maxItems = maxItems;
        }

        public Duration getPayTimeout() {
            return payTimeout;
        }

        public int getMaxItems() {
            return maxItems;
        }
    }
}
