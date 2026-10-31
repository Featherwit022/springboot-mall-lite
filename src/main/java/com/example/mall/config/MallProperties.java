package com.example.mall.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.Duration;

@ConstructorBinding
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
