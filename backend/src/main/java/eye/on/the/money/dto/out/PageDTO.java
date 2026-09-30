package eye.on.the.money.dto.out;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageDTO<T>(List<T> content, long totalElements) {

    public static <T> PageDTO<T> of(Page<T> page) {
        return new PageDTO<>(page.getContent(), page.getTotalElements());
    }
}
