package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SaveProductRequest {
    private String productId;
    private String type;
}
